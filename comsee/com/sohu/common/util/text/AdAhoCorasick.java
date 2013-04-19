package com.sohu.common.util.text;
//////////////////////////////////////////////////////////////////////
//
//		$Revision: 1.3 $
//		$Author: wangying $
//		$Date: 2006/08/25 11:14:53 $
//
//////////////////////////////////////////////////////////////////////



import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public abstract class AdAhoCorasick implements StringMatcher
{
	protected static Comparator<tTrieNode> tTrieNodeComparator = new Comparator<tTrieNode>(){
		@Override
		public int compare(tTrieNode o1, tTrieNode o2) {
			int rv = o1.key - o2.key;
			if( rv > 0 ) return 1;
			else if ( rv < 0 ) return -1;
			else
				return 0;
		}
	};
	protected class tTrieNode
	{
		public int key;
		//可以判定名中的关键词ID集合
		public List<Integer> mMatchedPatterns;
		public List<tTrieNode> mChildren;
		public tTrieNode mSupply;

		public tTrieNode()
		{
			mMatchedPatterns = null;
			mChildren = new LinkedList<tTrieNode>();
			mSupply = null;
		}
		public tTrieNode hasChildren(tTrieNode k, boolean doAdd){
			int t = Collections.binarySearch(mChildren, k, tTrieNodeComparator);
			if( t >= 0 ){
				return mChildren.get(t);
			} else {
				if( doAdd ){
					int pos = -t -1;
					tTrieNode tmp = new tTrieNode();
					tmp.key = k.key;
					mChildren.add(pos, tmp);
					return mChildren.get(pos);
				}
			}
			return null;
		}
		public tTrieNode insertChildren(tTrieNode k){
			int t = Collections.binarySearch(mChildren, k, tTrieNodeComparator);
			if( t >= 0 ){
				return mChildren.get(t);
			} else {
				int pos = -t -1;
				mChildren.add(pos, k);
				return k;
			}
		}
	}

	protected tTrieNode root;

	public AdAhoCorasick()
	{
		root = null;
	}

//	private static final int INDEX_OFFSET = 128;

	@Override
	public int initialize(byte keywords[][], int offset, int keywordnum)
	{
		//build the basic trie
		root = new tTrieNode();
		tTrieNode tmp = new tTrieNode();
		for (int i = 0; i < keywordnum; i++)
		{
			tTrieNode pvisit = root;
			int kindex = i + offset;
			if( keywords[kindex] == null )
				continue;
			for (int j = 0; j < keywords[kindex].length; j++)
			{
				tmp.key = keywords[kindex][j];
				pvisit = pvisit.hasChildren(tmp, true);
			}
			if (pvisit.mMatchedPatterns == null)
			{
				pvisit.mMatchedPatterns = new LinkedList<Integer>();
			}
			pvisit.mMatchedPatterns.add(i);
		}

		//set the supply links
		LinkedList<tTrieNode> nodequeue = new LinkedList<tTrieNode>();
		nodequeue.addFirst(root);
		while (nodequeue.size() > 0)
		{
			tTrieNode parent = (tTrieNode) nodequeue.pollLast();

			for (int i = 0; i < parent.mChildren.size(); i++)
			{
				tTrieNode child = parent.mChildren.get(i);
				if (child == null)
				{
				}
				else
				{
					nodequeue.addFirst(child);
					
					tTrieNode down = parent.mSupply;
					while ((down != null) && (down.hasChildren(child,false) == null))
					{
						down = down.mSupply;
					}
					if (down != null)
					{
						child.mSupply = down.hasChildren(child,false);
						if (child.mSupply.mMatchedPatterns != null)
						{
							//将child.mSupply.mMatchedPatterns复制到child.mMatchedPatterns
							if( child.mMatchedPatterns == null )
								child.mMatchedPatterns = child.mSupply.mMatchedPatterns;
							else 
								child.mMatchedPatterns.addAll(child.mSupply.mMatchedPatterns);
						}
					}
					else
					{
						child.mSupply = root;
					}
				}
			}

		}
		
		
		//precompute all the transistions simulated by the supply function
		//and set all null transition to the trie root
		nodequeue.addFirst(root);
		while (nodequeue.size() > 0)
		{
			tTrieNode current = (tTrieNode)nodequeue.pollLast();
			
			for (int i = 0; i < current.mChildren.size(); i ++)
			{
				if (current.mChildren.get(i) != null)
				{
					nodequeue.addFirst(current.mChildren.get(i));
				}
			}
			if( current.mSupply == null ) 
				current.mSupply = root;
			else{
				for (tTrieNode ch: current.mSupply.mChildren)
				{
					current.insertChildren(ch);
				}
			}
		}

		return 1;
	}

	@Override
	public void clear()
	{
	}

	@Override
	public int search(byte text[], int offset, int scanlen, Object obj)
	{
		if (root == null)
		{
			return 0;
		}
		
		tTrieNode tmp = new tTrieNode();
		tTrieNode visit = root;
		int scanned = offset + scanlen;
		for (int i = offset; i < scanned; i ++)
		{
			tmp.key = text[i];
			tTrieNode tv = visit.hasChildren(tmp,false);
			if( tv == null ) tv = visit.mSupply;
			if( tv == null ) break;
			visit = tv;
			if (visit.mMatchedPatterns != null)
			{
				for (int j = 0; j < visit.mMatchedPatterns.size(); j ++)
				{
					int ret = hit_report(visit.mMatchedPatterns.get(j), i - offset, obj);
					if (ret < 0)
					{
						/** oro **/
						return ret;
					}
				}
			}
			
		}
		return 1;
	}

	@Override
	public abstract int hit_report(int iPattern, int iPos, Object obj);
}
