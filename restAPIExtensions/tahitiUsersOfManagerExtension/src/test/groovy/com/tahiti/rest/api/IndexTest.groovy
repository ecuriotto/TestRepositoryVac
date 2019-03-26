package com.tahiti.rest.api;
import groovy.json.JsonSlurper
import groovy.mock.interceptor.MockFor;
import javax.servlet.http.HttpServletRequest
import org.bonitasoft.engine.identity.User
import org.bonitasoft.engine.identity.UserSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.impl.SearchResultImpl;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import spock.lang.Specification
import com.bonitasoft.engine.api.APIClient;
import com.bonitasoft.engine.api.IdentityAPI;
import com.bonitasoft.web.extension.rest.RestAPIContext
/**
* @see http://spockframework.github.io/spock/docs/1.0/index.html
*/
class IndexTest extends Specification {
// Declare mocks here
// Mocks are used to simulate external dependencies behavior
def HttpServletRequest httpRequest = Mock()
def ResourceProvider resourceProvider = Mock()
def RestAPIContext context = Mock()
def APIClient apiClient = Mock();
def APISession apiSession = Mock();
def IdentityAPI identityAPI = Mock();
/**
* You can configure mocks before each tests in the setup method
*/
def setup(){
// Simulate access to configuration.properties resource
context.resourceProvider >> resourceProvider
resourceProvider.getResourceAsStream("configuration.properties") >>
IndexTest.class.classLoader.getResourceAsStream("testConfiguration.properties")
context.apiClient >> apiClient
context.apiSession >> apiSession
apiSession.userId >> 1L
apiClient.identityAPI >> identityAPI
}
public void should_return_an_empty_userlist_when_logged_user_is_not_manager() {
given: "a RestAPIController"
def index = new Index()
httpRequest.getParameter("p") >> 0
httpRequest.getParameter("c") >> 100
identityAPI.searchUsers(new SearchOptionsBuilder(0, 100)
.filter(UserSearchDescriptor.MANAGER_USER_ID, apiSession.userId).done()) >> new SearchResultImpl<User>(0,[])

when: "Invoking the REST API"
def apiResponse = index.doHandle(httpRequest, new RestApiResponseBuilder(),
context)
then: "A JSON representation is returned in response body"
def jsonResponse = new JsonSlurper().parseText(apiResponse.response)
// Validate returned response
apiResponse.httpStatus == 200
//jsonResponse == []
jsonResponse == "You are not a manager. Log in as a manger to see your employees."
}
public void should_return_a_userlist_when_logged_user_is_a_manager() {
given: "a RestAPIController"
def index = new Index()
httpRequest.getParameter("p") >> 0
httpRequest.getParameter("c") >> 100
identityAPI.searchUsers(new SearchOptionsBuilder(0, 100)
.filter(UserSearchDescriptor.MANAGER_USER_ID, apiSession.userId).done()) >> new SearchResultImpl<User>(2,[Mock(User),Mock(User)])
when: "Invoking the REST API"
def apiResponse = index.doHandle(httpRequest, new RestApiResponseBuilder(),
context)
then: "A JSON representation is returned in response body"
def jsonResponse = new JsonSlurper().parseText(apiResponse.response)
// Validate returned response
apiResponse.httpStatus == 200
jsonResponse.size() == 2
}
}