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

//@CompileStatic
class MyCloudConnector extends CloudConnector {
	static final CT_JSON = 'application/json'
	JsonSlurper slurper = new JsonSlurper()
	static final dateDecoder = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZoneUTC().withOffsetParsed()
	
	@Override
	Or<RequestDef, Failure> signAndPrepare(Context ctx, RequestDef req, DeviceInfo info, Phase phase) {
		switch (phase) {
			case Phase.getOauth2Code:
			case Phase.getOauth2Token:
				def params = [:] 
					params.putAll(req.queryParams())
					//params.remove('client_secret')
				return new Good(req.withQueryParams(params))
			case Phase.subscribe:
			case Phase.unsubscribe:
			case Phase.fetch:
				return new Good(req.addHeaders(["Authorization": "Bearer " + info.credentials.token]))
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
			/*	def valueToSend = paramsAsJson.value
				if (valueToSend == null) {
					return new Bad(new Failure("Missing field 'value' in action parameters ${paramsAsJson}"))
				}*/
				//def req = new RequestDef("${ctx.parameters().endpoint}/me/posts?limit=1&fields=message")
				def req = new RequestDef("https://graph.facebook.com/me/posts?limit=1")
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
		def ts = DateTime.parse(jsData.created_time, dateDecoder).getMillis() as long 
		return ts
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
				//	print("\n======json========\n"+json+"\n==================\n")
				//	print("\n=======jsontype=====\n"+json.getClass()+"\n================\n")
				//	print("\n=======json.dataType=====\n"+json.data.getClass()+"\n================\n")
				//	print("\n=======json.data.collectType=====\n"+json.data.collect.getClass()+"\n================\n")

				//	print("\n=======json.data.collect =====\n"+json.data.collect+"\n================\n")
				//	print("\n=======json.collectManyType =====\n"+json.collectMany.getClass()+"\n================\n")
					def events = json.data.collect { jsData ->
						def js = denull(jsData)
				//		print("\n=======jsData=========\n"+jsData+"\n==================\n")
				//		print("\n=======jsDatatype=====\n"+jsData.getClass()+"\n================\n")
						new Event(retrieveTimestampFromRequest(req, js), JsonOutput.toJson(js))
					}
				//	print("\n========eventType======\n"+events.getClass()+"\n===================\n")
					return new Good(events)
				 }
				 return new Bad(new Failure("unsupported response ${res} ... ${res.contentType} .. ${res.contentType.startsWith("application/json")}"))
			default:
				return new Bad(new Failure("http status : ${res.status} is not OK (${HTTP_OK})"))
				
		}
	}

}
