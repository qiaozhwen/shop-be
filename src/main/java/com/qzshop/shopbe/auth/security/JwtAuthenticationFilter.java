package com.qzshop.shopbe.auth.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.qzshop.shopbe.auth.token.JwtService;
import com.qzshop.shopbe.auth.token.ParsedToken;
import com.qzshop.shopbe.auth.token.SubjectType;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthenticationFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                ParsedToken p = jwt.parse(header.substring(7));
                List<SimpleGrantedAuthority> auths = new ArrayList<>();
                if (p.type() == SubjectType.STAFF && p.roles() != null) {
                    for (String r : p.roles()) {
                        auths.add(new SimpleGrantedAuthority("ROLE_" + r));
                    }
                }
                auths.add(new SimpleGrantedAuthority("TYPE_" + p.type().name()));
                StaffPrincipal principal = new StaffPrincipal(p.subjectId(), p.roles());
                var auth = new UsernamePasswordAuthenticationToken(principal, null, auths);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // 不阻断，由后续 401/403 处理
            }
        }
        chain.doFilter(req, resp);
    }
}
