package io.ezto.bg.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class RootController {
    @GetMapping(value = "/")
    public void redirectRootToSwaggerDocs(HttpServletResponse response) throws IOException {
        response.sendRedirect("/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config");
    }
}
