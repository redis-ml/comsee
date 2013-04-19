package com.sohu.sohudb;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.sohu.common.connectionpool.async.AsyncRequest;

public class SohuDBRequest extends AsyncRequest {

	private volatile byte[] key;
	private volatile int keyLen = -1;
	private volatile  RequestType cmd;
	private volatile byte[] value;
	private volatile int valueLen = -1;
	private volatile int flag; 

	private volatile int forceId;
	/**
	 * @return the forceId
	 */
	public int getForceId() {
		return forceId;
	}

	/**
	 * @param forceId the forceId to set
	 */
	public void setForceId(int forceId) {
		this.forceId = forceId;
	}

	@Override
	public int getServerId(int total) {
		byte[] data = key;
		if( data == null || total <= 0) return -1;
		
		if( forceId >= 0 ){
			return forceId % total;
		}
		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(data);
			int b = (int)md.digest()[0];
			return (b>0?b+256:b)%total;
		} catch(NoSuchAlgorithmException e ){
		}
		return -1;
	}

	/**
	 * 检查请求是否正常：命令必须为已知命令，同时key不能为空。
	 */
	@Override
	public boolean isValid() {
		return (cmd != null && cmd != RequestType.CMD_UNKNOWN)
				&& (key != null);
	}

	/**
	 * @return the key
	 */
	public byte[] getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(byte[] key) {
		this.key = key;
	}

	/**
	 * @return the cmd
	 */
	public RequestType getCmd() {
		return cmd;
	}

	/**
	 * @param cmd the cmd to set
	 */
	public void setCmd(RequestType cmd) {
		this.cmd = cmd;
	}

	/**
	 * @return the value
	 */
	public byte[] getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(byte[] value) {
		this.value = value;
	}

	/**
	 * @return the keyLen
	 */
	public int getKeyLen() {
		return keyLen;
	}

	/**
	 * @param keyLen the keyLen to set
	 */
	public void setKeyLen(int keyLen) {
		this.keyLen = keyLen;
	}

	/**
	 * @return the valueLen
	 */
	public int getValueLen() {
		return valueLen;
	}

	/**
	 * @param valueLen the valueLen to set
	 */
	public void setValueLen(int valueLen) {
		this.valueLen = valueLen;
	}

	/**
	 * @return the flag
	 */
	public int getFlag() {
		return flag;
	}

	/**
	 * @param flag the flag to set
	 */
	public void setFlag(int flag) {
		this.flag = flag;
	}
	
	private String reqtype="snapshot";
	
	public void setReqType(String reqtype){
		this.reqtype=reqtype;
	}
	public String getReqType(){
		return reqtype;
	}
}
