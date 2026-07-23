package com.devteria.chat.repository.httpclient;

import com.devteria.chat.dto.ApiResponse;
import com.devteria.chat.dto.request.IntrospectRequest;
import com.devteria.chat.dto.response.IntrospectResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="identity-client" ,url = "${app.services.identity.url}")
public interface identityClient {
    @PostMapping("/auth/introspect")
    ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request);
}
