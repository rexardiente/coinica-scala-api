
#### import classes
	play.api.libs.ws.WSClient // inject to Controller
	models.domain.eosio.TableRowsRequest
	utils.lib.EOSIOSupport

#### Get All GQ Active Users
	val req = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
    new utils.lib.EOSIOSupport(ws).getGQUsers(req).map(Ok(_))

> Note: Make TableRowsRequest as dynamic class object that can handle all EOSIO related table requests.