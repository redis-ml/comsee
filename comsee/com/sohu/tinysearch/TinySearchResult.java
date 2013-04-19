/*
 * Created on 2005-11-24
 *
 */
package com.sohu.tinysearch;

import java.util.List;

import com.sohu.common.connectionpool.Result;

/**
 * tinysearch查询的结果
 * 
 * @author Mingzhu Liu
 *  
 */
public class TinySearchResult implements Result{
	// 查询结果串
	List items = null;
	// 查询用时
	long time = 0;
	
	// 广告结果串
	TinySearchResultItem adItems[] ;

	// 广告结果的最大数

	// QueryCorrection的类型
	// 0表示忽略qr值
	// 1表示拼音
	// 2表示纠错
	int qrType = 0;
	// qr 条数
	int qrNum = 0;
	// qr的内容, 用'\t'分开
	String qr = null;
	// 原始的qr头
	String qrHeader = null;
	
	// Hint的类型(还没用)
	int hintType = 0;
	// Hint 条数
	int hintNum = 0;
	// hint的内容,用'\t'分开
	String hint = null;
	// 原始的hint头
	String hintHeader = null;
	//tinyEngine，原始的qa头部信息
	String qaHeader = null;
	//tinyEngine，经过后台处理返回的查询词
	String qaWord = null;
	//tinyEngine，类别id，64位，每位标明一个类别，目前没有多个类别
	//当类别等于-1的时候，表示为问答类，要和cache协同判断，两个都是才行
	long qaCatid = 0;
	//tinyEngine，特征id，32位，每位标明一个特征，可能有多个特征并存
	int qaSignid = 0;

	// 第一个有效的结果的索值
	int firstValid = 0;
	
	public List getItems() {
		return items;
	}
	public void setItems(List items) {
		this.items = items;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public int getFirstValid() {
		return firstValid;
	}
	public void setFirstValid(int firstValid) {
		this.firstValid = firstValid;
	}
	public int getQrType(){
		return this.qrType;
	}
	public void setQrType(int qt){
		this.qrType = qt;
	}
//	这一部分是tinyEngine的代码
	public void setQaHeader(String header) {
		this.qaHeader = header;
	}
	public String getQaHeader() {
		return this.qaHeader;
	}
	public void setQaCategory(Long catid) {
		this.qaCatid = catid;
	}
	public Long getQaCategory() {
		return this.qaCatid;
	}
	public void setQaSignid(int sid) {
		this.qaSignid = sid;
	}
	public int getQaSignid() {
		return this.qaSignid;
	}
	public void setQaWord(String qword) {
		//TODO：maybe need some handle
		this.qaWord = qword;
	}
	public String getQaWord() {
		return this.qaWord;
	}
	
//	private boolean isQrChecked = false;//标记是否经过了屏蔽词审查
//	private boolean isQrClear = true;
//	private boolean isHintChecked = false; 
//	private boolean isHintClear = true; //标记是否经过了屏蔽词审查
//	Perl5Matcher matcher = null;

//	public static String fuck(String query , char deli, Perl5Matcher matcher){
//		if( query != null ){
//			StringBuffer sb = new StringBuffer();
//			int left=0;
//			do {
//				int right = query.indexOf( deli, left );
//				
//				String term;
//				if( right== -1 ) right = query.length();
//				if( right<left ){
//					term = query.substring(left);
//				} else if( right==left ){
//					left = right+1;
//					continue;
//				} else{
//					term = query.substring(left, right );
//				}
//				if( sb.length() !=0 ){
//					sb.delete(0, sb.length() );
//				}
//				if(  PageMask.getInstance().matches(term, false, matcher) 
//						|| NewBlackList.getInstance().matches( term, matcher, sb)!=null ){
//					return null;
////					if( sb == null ){
////						sb = new StringBuffer( query.length() );
////						for( int i=0; i< left; i++ ){
////							sb.append( query.charAt(i) );
////						}
////					}
////					
////				} else {
////					if( sb != null ){
////						if( sb.length()>0 ){
////							sb.append('\t');
////						}
////						sb.append( term );
////					}
//				}
//				left = right+1;
//			}while( left< query.length());
//			
////			if( sb != null ){
////				this.qr = sb.toString();
////			}
//		}
//		return query;
//	}
	public String getQr(){
		return this.qr;
	}
	public void setQr(String qr){
		this.qr = qr;
	}
	public void setHintType( int ht){
		this.hintType = ht;
	}
	public int getHintType ( ) {
		return this.hintType;
	}
	public String getHint() {
		return this.hint;
	}
	public void setHint(String hint ){
		this.hint = hint;
	}
	public String getQrHeader(){
		return this.qrHeader;
	}
	public void setQrHeader( String qh ){
		this.qrHeader = qh;
	}
	public String getHintHeader(){
		return this.hintHeader;
	}
	public void setHintHeader(String hh ){
		this.hintHeader = hh;
	}
	public int getHintNum(){
		return this.hintNum;
	}
	public void setHintNum(int hn ){
		this.hintNum = hn;
	}
	public int getQrNum(){
		return this.qrNum;
	}
	public void setQrNum( int qn){
		this.qrNum = qn;
	}
	public void setAdItems(TinySearchResultItem[] adItems) {
		this.adItems = adItems;
	}
	public TinySearchResultItem getAdItem( int idx ){
		TinySearchResultItem adItems[] = this.adItems;
		if( adItems == null ) return null;
		if( idx <0 || adItems.length <= idx ) return null;
		
		return adItems[ idx ];
	}
	public void setAdItem( int idx, TinySearchResultItem item) throws IndexOutOfBoundsException {
		if( item == null ) return;
		
		if( idx > TinySearch.MAX_AD_ITEM_NUM || idx <0 ) throw new IndexOutOfBoundsException( ""+idx );
		
		if( this.adItems == null ){
			this.adItems = new TinySearchResultItem [ TinySearch.MAX_AD_ITEM_NUM ];
		}
		this.adItems [ idx ] = item ;
	}
}
