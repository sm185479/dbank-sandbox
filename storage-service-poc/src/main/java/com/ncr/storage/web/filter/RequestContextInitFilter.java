package com.ncr.storage.web.filter;

import com.intuit.ifs.afeLibrary.util.dto.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Request context initialization
 *
 */
@Slf4j
public class RequestContextInitFilter extends OncePerRequestFilter {
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if(request.getRequestURL().lastIndexOf("health") > 0){
			filterChain.doFilter(request, response);
			return;
		}

		try {
			RequestContext.clear();

			String bcId = request.getHeader("bcId");
			RequestContext.setBcId(bcId);
			RequestContext.put("canonicalId", bcId);
			RequestContext.put("bcIndex", StringUtils.right(bcId, 2));

			filterChain.doFilter(request, response);
		} finally {
			response.setCharacterEncoding("UTF-8");
			RequestContext.clear();
		}
	}
}
