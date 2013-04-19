package com.sohu.sohudb;

/**
 * SOHU DB的命令类型
 * @author Administrator
 *
 */
public enum RequestType {

	CMD_UNKNOWN,// 不支持的命令
	CMD_GET,   // 获取数据
	CMD_PUT,   // 写数据
	CMD_DEL,   // 删除数据
	CMD_SYNC,  // 发送同步信号
	CMD_QGET,  // （未知）
	CMD_GETSIZE, //（未知）
	CMD_GETMETA, //（未知）
}
