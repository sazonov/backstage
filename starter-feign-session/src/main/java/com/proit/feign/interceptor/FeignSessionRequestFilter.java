package com.proit.feign.interceptor;

import com.proit.app.utils.SecurityUtils;
import com.proit.feign.provider.PrincipalProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.proit.feign.properties.FeignSessionProperties.GLOBAL_INTERCEPTOR_ACTIVATION_PROPERTY;

@Component
@ConditionalOnProperty(value = GLOBAL_INTERCEPTOR_ACTIVATION_PROPERTY, matchIfMissing = true)
@RequiredArgsConstructor
public class FeignSessionRequestFilter extends OncePerRequestFilter
{
	private final PrincipalProvider principalProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException
	{
		var userId = request.getHeader(RequestHeaders.USER_ID.getName());

		if (userId == null)
		{
			filterChain.doFilter(request, response);

			return;
		}

		var principal = principalProvider.getPrincipal(userId);

		SecurityUtils.runForUser(principal, () -> filterChain.doFilter(request, response));
	}
}
