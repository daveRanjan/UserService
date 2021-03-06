package com.cclogic.Security;

/**
 * Created by Nishant on 9/18/2017.
 */

import com.cclogic.Exceptions.ResourceNotFoundException;
import com.cclogic.User.User;
import com.cclogic.User.UserHeaderTokenData;
import com.cclogic.User.UserRepository;
import com.cclogic.User.UserService;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.datetime.joda.JodaTimeContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import static java.util.Collections.emptyList;

@Component
public class TokenAuthenticationService {
    static final long EXPIRATION_TIME = 864_000_000; // 10 days
    static final String SECRET = "ThisIsASecret";
    static final String TOKEN_PREFIX = "Bearer";
    public static final String HEADER_STRING = "Authorization";


    private static UserService instance;

    @Autowired
    private UserService userService;

    @PostConstruct
    public void init(){
        TokenAuthenticationService.instance = userService;
    }


    public static void addAuthentication(HttpServletResponse res, String username) {

        System.out.println("UserName at addAuthentication : "+username);


        User user = instance.getUserByEmail(username);


        if(user==null){
            throw new ResourceNotFoundException("An unexpected exception. Try to login again");
        }

        Gson gson = new Gson();

        HashMap<String, Object> params = new HashMap<>();
        params.put("sub", username);
        params.put("userid",user.getId().toString());
        params.put("role",user.getUserType());
        params.put("issue", new Date(System.currentTimeMillis()/1000));

        String JWT = Jwts.builder()
                .setClaims(params)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
        res.addHeader(HEADER_STRING, TOKEN_PREFIX + " " + JWT);

        HashMap<String,String> responseData = new HashMap<>();
        responseData.put("status",""+ HttpStatus.ACCEPTED);
        responseData.put("message","Login Successful");


        try {
            res.getOutputStream().println(gson.toJson(responseData));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Authentication getAuthentication(HttpServletRequest request) {
        String token = request.getHeader(HEADER_STRING);
        if (token != null) {
            // parse the token.
            Jws jws = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""));

            Claims claims = (Claims) jws.getBody();

            String user = claims.getSubject();

            //System.out.println("User Data from token: "+jws.toString());

            return user != null ?
                    new UsernamePasswordAuthenticationToken(user, null, emptyList()) :
                    null;
        }
        return null;
    }

    public static UserHeaderTokenData getUserData(String token){
        if (token != null) {
            // parse the token.
            Jws jws = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""));

            Claims claims = (Claims) jws.getBody();

            String user = claims.getSubject();

            int userId = Integer.parseInt(""+claims.get("userid"));
            String role = ""+claims.get("role");

            System.out.println("User Data -- from token: "+jws.toString());
            System.out.println("User Id : "+userId);
            System.out.println("Role : "+role);
            System.out.println("username : "+user);

            UserHeaderTokenData userHeaderTokenData = new UserHeaderTokenData();
            userHeaderTokenData.setId(userId);
            userHeaderTokenData.setRole(role);

            return userHeaderTokenData;
        }
        return null;
    }
}
