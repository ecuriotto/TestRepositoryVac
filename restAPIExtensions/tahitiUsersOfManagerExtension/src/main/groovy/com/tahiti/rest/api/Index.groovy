package com.tahiti.rest.api;

import groovy.json.JsonBuilder

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.apache.http.HttpHeaders
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import org.bonitasoft.engine.session.APISession
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.api.TenantAPIAccessor
import org.bonitasoft.engine.identity.UserSearchDescriptor

class Index implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        // To retrieve query parameters use the request.getParameter(..) method.
        // Be careful, parameter values are always returned as String values
		// Retrieve pagination parameters
		LOGGER.info("Start rest api");
		int p = 0;
		int c = 10;
		
		try{
		p = Integer.parseInt(request.getParameter("p"));
		c = Integer.parseInt(request.getParameter("c"));
		}
		catch(Exception e){
			LOGGER.info("Request parameters p and c should be numbers");
			//return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, null);
		}
		
		def apiSession = context.getApiSession();
		def managerUserId = apiSession.getUserId();
		
		def contextApiClient = context.getApiClient();		
		// Get a reference to IdentityAPI to search for users managed by current user
		def identityAPI = contextApiClient.getIdentityAPI();
		
		
		def searchBuilder = new SearchOptionsBuilder(p*c, c);
		searchBuilder.filter(UserSearchDescriptor.MANAGER_USER_ID, managerUserId)
		def users = identityAPI.searchUsers(searchBuilder.done()).getResult()
		LOGGER.info "Users managed by: " + managerUserId + " are: " + users
		// If current user manages at least one user he/she is a manager
		
		def userListNumber = 0;
		if(!users.empty){
			userListNumber = users.size();
		}else{
			users = "You are not a manager. Log in as a manger to see your employees."
		}
        // users variable is what we want to send in the response
        		
        // Send the result as a JSON representation
        // You may use buildPagedResponse if your result is multiple
        return buildPagedResponse(responseBuilder, new JsonBuilder(users).toString(),p, c, new Long(userListNumber))
    }

    /**
     * Build an HTTP response.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  httpStatus the status of the response
     * @param  body the response body
     * @return a RestAPIResponse
     */
    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }

    /**
     * Returns a paged result like Bonita BPM REST APIs.
     * Build a response with content-range data in the HTTP header.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  body the response body
     * @param  p the page index
     * @param  c the number of result per page
     * @param  total the total number of results
     * @return a RestAPIResponse
     */
    RestApiResponse buildPagedResponse(RestApiResponseBuilder responseBuilder, Serializable body, int p, int c, long total) {
        return responseBuilder.with {
            withAdditionalHeader(HttpHeaders.CONTENT_RANGE,"$p-$c/$total");
            withResponse(body)
            build()
        }
    }

    /**
     * Load a property file into a java.util.Properties
     */
    Properties loadProperties(String fileName, ResourceProvider resourceProvider) {
        Properties props = new Properties()
        resourceProvider.getResourceAsStream(fileName).withStream { InputStream s ->
            props.load s
        }
        props
    }

}
