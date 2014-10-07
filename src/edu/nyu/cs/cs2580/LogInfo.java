package edu.nyu.cs.cs2580;

public class LogInfo {
	public int _session_id;
	public String _query;
	public int _did;
	public String _action;
	public long _time;

	LogInfo(int session_id,String query,int did, String action, long time){
		_session_id=session_id;
		_query=query;
		_did = did;
		_action = action;
		_time = time;
	}

	String asString(){
		return new String(
				Integer.toString(_session_id)+"\t"+_query+Integer.toString(_did) + "\t" + _action + "\t" + Long.toString(_time));
	}
}
