package reader;

import java.io.IOException;
import java.util.Arrays;

public class RawData {
	
	public byte data[];
	public int size;
	public RawData linked;
	
	public RawData(){}
	public RawData(int size, byte data[]){
		this.size=size;
		if(data.length!=size){
			this.data=Arrays.copyOf(data, size);
		}else{
			this.data=data;
		}
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder("");
		RawData tl=this;
		while(tl!=null&&tl.data!=null){
			builder.append(new String(tl.data));
			tl=tl.linked;
		}
		return builder.toString();
	}
	
	public long getSize(){
		long size = 0;
		RawData tl=this;
		while(tl!=null&&tl.data!=null){
			size+=tl.size;
			tl=tl.linked;
		}
		return size;
	}
	
	public byte[] getSolidBlock() throws IOException{
		//assume this is a flat data/ only one link
		if(this.linked==null){
			return this.data;
		}
		long length = getSize();
		if(length>Integer.MAX_VALUE){
			throw new IOException("Data Contained Exceeds the Size of An Integer, Cannot Align To A Solid Block Of Memroy");
		}
		byte[] rtn = new byte[(int) length];
		
		int offset = 0;
		
		RawData tl=this;
		while(tl!=null&&tl.data!=null){
			for(int i = 0; i < size; i++){
				rtn[offset] = data[i];
				offset++;
			}
			tl=tl.linked;
		}
		
		return rtn;
	}
	
	public byte[] setDataToSolidBlock() throws IOException{
		long length = getSize();
		if(length>Integer.MAX_VALUE){
			throw new IOException("Data Contained Exceeds the Size of An Integer, Cannot Align To A Solid Block Of Memroy");
		}
		byte[] rtn = new byte[(int) length];
		
		int offset = 0;
		
		RawData tl=this;
		while(tl!=null&&tl.data!=null){
			for(int i = 0; i < size; i++){
				rtn[offset] = data[i];
				offset++;
			}
			tl=tl.linked;
		}
		
		this.linked=null;
		this.data = rtn;
		this.size = (int) length;
		
		return rtn;
	}
	
}
