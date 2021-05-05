const moment = require('moment'); // require
const ConsumerServer = require('chronicle-consumer');
const { v4: uuidv4,  parse: uuidParse } = require('uuid');
const { Pool, Client } = require('pg');
const WebSocket = require('ws');
const ws = new WebSocket('ws://3.36.96.196:9001/ws');

ws.on('open', function open() {
	let msg = {id:"chronicle-consumer", message:"subscribe"};
  ws.send(JSON.stringify(msg));
});
ws.on('message', function incoming(data) {
  console.log("ws.on message");
});

const server = new ConsumerServer({port: 8800});
const pool = new Pool({
  user: 'eos-game-store-api',
  host: '37.44.244.221',
  database: 'eos-game-store-api',
  password: 'eos-game-store-api',
  port: 5432,
})

server.on('fork', function(data) {
    let block_num = data['block_num'];
    console.log('fork: ' + block_num);
});

// the pool will emit an error on behalf of any idle clients
// it contains if a backend error or network partition happens
pool.on('error', (err, client) => {
  console.error('Unexpected error on idle client', err)
  process.exit(-1)
})

const strToFloat = (str) => parseFloat(str)
const strToInt = (str) => parseInt(str)
const strToBool = (str) => (str == 'true')

server.on('tx', function(data) {
    let tx_printed = false;
    let trace = data.trace;
    if(trace.status == 'executed') {
        for(let i=0; i< trace.action_traces.length; i++) {
            let atrace = trace.action_traces[i];
            if(atrace.receipt.receiver == atrace.act.account) {
                if(atrace.act.name == 'transfer') {
                    if(!tx_printed) {
                        // console.log('tx: ' + trace.id);
                        ;(async () => {
						  const client = await pool.connect()
						  try {
						    const checker = await client.query('SELECT * FROM "EOS_NET_TRANSACTION" WHERE "TRACE_ID"=$1', [trace.id])

						    if (checker.rowCount == 0) {
						    	// Construct DB tbl values, insert and validate response
						    	// failed tx's will be written to a log file
						    	const new_uuid = uuidv4()
						    	const block_num = strToFloat(data.block_num)

						    	let atrace = data.trace
						    	atrace.cpu_usage_us = parseFloat(atrace.cpu_usage_us)
						    	atrace.net_usage_words = parseInt(atrace.net_usage_words)
						    	atrace.net_usage_words = parseInt(atrace.net_usage_words)
						    	atrace.elapsed = parseInt(atrace.elapsed)
						    	atrace.net_usage = parseInt(atrace.net_usage)
						    	atrace.scheduled = strToBool(atrace.scheduled)

						    	atrace.account_ram_delta == null || atrace.account_ram_delta == '' ? delete atrace.account_ram_delta : atrace.account_ram_delta
						    	atrace.except == null || atrace.except == '' ? delete atrace.except : atrace.except
						    	atrace.error_code == null || atrace.error_code == '' ? delete atrace.error_code : atrace.error_code

						    	let partial = atrace.partial
						    	partial.expiration = moment(partial.expiration).unix()
						    	partial.ref_block_num = parseFloat(partial.ref_block_num)
						    	partial.ref_block_prefix = parseFloat(partial.ref_block_prefix)
						    	partial.max_net_usage_words = parseInt(partial.max_net_usage_words)
						    	partial.max_cpu_usage_ms = parseInt(partial.max_cpu_usage_ms)
						    	partial.delay_sec = parseInt(partial.delay_sec)

						    	let action_traces = trace.action_traces
						    	action_traces.forEach((action) => {
						    		action.action_ordinal = parseInt(action.action_ordinal)
						    		action.creator_action_ordinal = parseInt(action.creator_action_ordinal)
						    		action.context_free = strToBool(action.context_free)
						    		action.elapsed = parseInt(action.elapsed)
						    		action.account_ram_deltas == null ? delete action.account_ram_deltas : action.account_ram_deltas
						    		action.console == '' ? delete action.console : action.console
						    		action.except == '' ? delete action.except : action.except
						    		action.error_code == '' || action.error_code == null ? delete action.error_code : action.error_code

						    		let receipt = action.receipt
						    		receipt.global_sequence = parseFloat(receipt.global_sequence)
						    		receipt.recv_sequence = parseInt(receipt.recv_sequence)
						    		receipt.code_sequence = parseInt(receipt.code_sequence)
						    		receipt.abi_sequence = parseInt(receipt.abi_sequence)
						    	})

						    	// Insert to DB...
								const insertQuery = 'INSERT INTO "EOS_NET_TRANSACTION"("ID","TRACE_ID","BLOCK_NUM","BLOCK_TIMESTAMP","TRACE") VALUES($1,$2,$3,$4,$5) RETURNING *'
								const insertValues = [new_uuid, trace.id, block_num, moment.utc(data.block_timestamp).unix(), trace]

								// Check insertion status...
								client.query(insertQuery, insertValues, (err, res) => {
								  if (err) {
								    console.log(err.stack)
								  } else {
								  	// notify server API regarding the update on EOS_NET_TRANSACTION..
								  	// send new_uuid and check if what tx "GQ, TH, MJ and etc."
								  	try {
								  		let new_tx = {id:"chronicle-consumer", input:{EOS_NET_TRANSACTION: new_uuid.toString()}};
										  ws.send(JSON.stringify(new_tx));
							    		// console.log(new_tx);
									    // console.log(res.rows[0]);
								  	} catch (e) {
								  		console.log("Server API not running");
								  	}
								  }
								})
							}
						  } finally {
						    // Make sure to release the client before any error handling,
						    // just in case the error handling itself throws an error.
						    client.release()
						  }
						})().catch(err => console.log(err.stack))
                        tx_printed = true;
                    }
                }
            }
        }
    }
});

server.start();