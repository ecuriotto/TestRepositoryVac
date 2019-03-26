package com.tahiti.rest.api;

import java.time.format.DateTimeFormatter;

import groovy.json.JsonBuilder

import javax.servlet.http.HttpServletRequest


import javax.servlet.http.HttpServletResponse

import org.apache.http.HttpHeaders
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.log.Log;
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import org.bonitasoft.engine.bdm.BusinessObjectDAOFactory
import org.bonitasoft.engine.session.APISession
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.api.TenantAPIAccessor
import org.bonitasoft.engine.identity.UserSearchDescriptor

import javax.servlet.http.HttpServletRequest

import com.company.model.VacationAvailable;
import com.company.model.VacationAvailableDAO;
import com.company.model.VacationRequest;
import com.company.model.VacationRequestDAO;

class Index implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class)


    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        // To retrieve query parameters use the request.getParameter(..) method.
        // Be careful, parameter values are always returned as String values

        // Here is an example of how you can retrieve configuration parameters from a properties file
        // It is safe to remove this if no configuration is required
        Properties props = loadProperties("configuration.properties", context.resourceProvider)
        String paramValue = props["myParameterKey"]
		
		//////////////////////////////////////////////////////////////////////////////////////////
		final REVIEW_REQUEST_TASK_NAME = 'Review request'
		
		// User session to get the user Bonita id to search for managed employee vacation requests
		def apiSession = context.getApiSession()
		
		// Get a reference to IdentityAPI to search for users managed by current user
		def identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession)

		// Get a reference to ProcessAPI to search for task id based on process instance id
		def processAPI = TenantAPIAccessor.getProcessAPI(apiSession)

		// DAO to access vacation request and vacation available
		def BusinessObjectDAOFactory daoFactory = new BusinessObjectDAOFactory()
		def vacationRequestDAO = daoFactory.createDAO(apiSession, VacationRequestDAO.class)
		def vacationAvailableDAO = daoFactory.createDAO(apiSession, VacationAvailableDAO.class)

		// Current user id
		def managerUserId = apiSession.getUserId()
		LOGGER.info( "Manager id is: " + managerUserId);

		// Search for users managed by current user and build users id list
		def searchBuilder = new SearchOptionsBuilder(0, 100)
		searchBuilder.filter(UserSearchDescriptor.MANAGER_USER_ID, managerUserId)
		def users = identityAPI.searchUsers(searchBuilder.done()).getResult()
		LOGGER.info( "Users managed by: " + managerUserId + " are: " + users);

		// If current user manage at least one user he/she is a manager
		def isManager = !users.empty;

		// Initialize the list of pending vacation requests for all employees
		def employeesVacationRequests = []

		// Initialize the list of vacation available for all employees
		def employeesVacationAvailable = []

		// For each employee
		for(user in users) {
			// Current user id
			def employeeId = user.id

			// Get pending vacation request
			List currentUserVacationRequests = vacationRequestDAO.findByRequesterBonitaBPMId(employeeId, 0, 100)
			
			// For each vacation request of current employee
			for(currentUserVacationRequest in currentUserVacationRequests) {
				// Id of the task to approve/refuse pending vacation request
				// null for vacation request that are not pending
				def reviewRequestTaskId

				// If vacation request is pending
				if(currentUserVacationRequest.status == "pending") {
					
					// Search for "Review request" task id using "New Vacation Request" process instance id store in business variable "VacationRequest"
					def listTask = processAPI.getHumanTaskInstances(currentUserVacationRequest.newRequestProcessInstanceId, REVIEW_REQUEST_TASK_NAME, 0, 1);
					LOGGER.info( "getHumanTaskInstances works " );
					for(task in listTask) {
						reviewRequestTaskId = task.id;
					
						// Map with current user single pending vacation request
						def vacationRequestInfo = ["firstName": user.firstName, "lastName": user.lastName, "startDate": currentUserVacationRequest.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE), "returnDate": currentUserVacationRequest.returnDate.format(DateTimeFormatter.ISO_LOCAL_DATE), "numberOfDays": currentUserVacationRequest.numberOfDays, "status": currentUserVacationRequest.status, "taskId": reviewRequestTaskId]

						// Add current employee vacation request to the global list
						employeesVacationRequests << vacationRequestInfo
					}
				}
			}

			// Get vacation available
			List vacationAvailable = vacationAvailableDAO.findByBonitaBPMId(employeeId, 0, 100)

			def vacationAvailableInfo = ["firstName": user.firstName, "lastName": user.lastName, "daysAvailableCounter": (vacationAvailable.size()>0 ? vacationAvailable.get(0).daysAvailableCounter: 0)]

			// Add current employee vacation available to the global list
			employeesVacationAvailable << vacationAvailableInfo
		}

		// Prepare the result
		def result = ["isManager": isManager, "employeesVacationRequests": employeesVacationRequests, "employeesVacationAvailable": employeesVacationAvailable]
		//////////////////////////////////////////////////////////////////////////////////////////
		

        // Send the result as a JSON representation
        // You may use buildPagedResponse if your result is multiple
        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
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
