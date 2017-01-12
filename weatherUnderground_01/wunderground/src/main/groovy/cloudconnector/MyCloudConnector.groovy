// Sample CloudConnector, that can be used as a boostrap to write a new CloudConnector.
// Lot of code is commented, and everything is optional.
// The class can be named as you want no additional import allowed
// See the javadoc/scaladoc of cloud.artik.cloudconnector.api_v1.CloudConnector
package cloudconnector

import org.scalactic.*
import org.joda.time.format.DateTimeFormat
import org.joda.time.*
import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import scala.Option
import cloud.artik.cloudconnector.api_v1.*
import static java.net.HttpURLConnection.*
import java.lang.Integer


//@CompileStatic
class MyCloudConnector extends CloudConnector {
	static final CT_JSON = 'application/json'
	JsonSlurper slurper = new JsonSlurper()

	@Override
	Or<RequestDef, Failure> signAndPrepare(Context ctx, RequestDef req, DeviceInfo info, Phase phase) {
		switch (phase) {
			case Phase.getOauth2Code:
			case Phase.getOauth2Token:
				def params = [:] 
				params.putAll(req.queryParams())
				params.remove('authorizationUrl')
				params.remove('accessTokenUrl')
				params.remove('clientId')
				params.remove('client_secret')
				params.remove('scope')
				params.remove('statusAcceptNotification')
				params.remove('accessTokenMethod')
				params.remove('parameters')
				return new Good(req.withQueryParams(params))
			case Phase.subscribe:
			case Phase.unsubscribe:
			case Phase.fetch:
		//	case Phase.refreshToken:
		// 	case Phase.undef:
			default:
				return super.signAndPrepare(ctx, req, info, phase)
		}
	}



	@Override
	Or<ActionResponse, Failure> onAction(Context ctx, ActionDef action, DeviceInfo info) {
		switch (action.name) {
			case "getData":
				def paramsAsJson = slurper.parseText(action.params)
				def regionValue = paramsAsJson.region
				def cityValue = paramsAsJson.city

				if (regionValue == null || cityValue == null) {
					return new Bad(new Failure("Missing field 'value' in action parameters ${paramsAsJson}"))
				}
				def req = new RequestDef("http://api.wunderground.com/api/aa87622c46bdc178/conditions/q/"+ regionValue + "/" + cityValue + ".json")
					.withMethod(HttpMethod.Get)
					.withContentType("application/x-www-form-urlencoded")
				return new Good(new ActionResponse([new ActionRequest([req])]))
			default:
				return new Bad(new Failure("Unknown action: ${action.name}"))
		}
	}

	def denull(obj) {
		if(obj instanceof java.util.Map) {
			obj.collectEntries {k, v ->
				(v != null)? [(k): denull(v)] : [:]
			}
		} else if(obj instanceof java.util.Collection) {
			obj.collect { denull(it) }.findAll { it != null }
		} else {
			obj
		}
	}

	def long retrieveTimestampFromRequest(RequestDef req, jsData) {
		def ts = jsData.current_observation.local_epoch as long
		return ts * 1000
	}

	@Override
	Or<List<Event>, Failure> onFetchResponse(Context ctx, RequestDef req, DeviceInfo info, Response res) {
		switch (res.status) {
			case HTTP_OK:
				def content = res.content.trim()
				if (content == "") {
					ctx.debug("ignore response valid respond: '${res.content}'")
					return new Good(Empty.list())
				}
				else if (res.contentType.startsWith("application/json")) {
					def json = slurper.parseText(content)
					def ts = retrieveTimestampFromRequest(req, json)
					def events = json.current_observation.collect { jsData ->
						def js = denull(jsData)

						new Event(ts, JsonOutput.toJson(js))
					}
					return new Good(events)
				 }
				 return new Bad(new Failure("unsupported response ${res} ... ${res.contentType} .. ${res.contentType.startsWith("application/json")}"))
			default:
				return new Bad(new Failure("http status : ${res.status} is not OK (${HTTP_OK})"))
				
		}
	}
}
