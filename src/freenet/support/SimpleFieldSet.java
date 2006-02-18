package freenet.support;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import freenet.support.io.LineReader;

/**
 * @author amphibian
 * 
 * Very very simple FieldSet type thing, which uses the standard
 * Java facilities.
 */
public class SimpleFieldSet {

	final Map map;
    String endMarker;
    final boolean multiLevel;
    static public final char MULTI_LEVEL_CHAR = '.';
    
    public SimpleFieldSet(BufferedReader br, boolean multiLevel) throws IOException {
        map = new HashMap();
        this.multiLevel = multiLevel;
        read(br);
    }

    public SimpleFieldSet(LineReader lis, int maxLineLength, int lineBufferSize, boolean multiLevel) throws IOException {
    	map = new HashMap();
    	this.multiLevel = multiLevel;
    	read(lis, maxLineLength, lineBufferSize);
    }
    
    /**
     * Empty constructor
     */
    public SimpleFieldSet(boolean multiLevel) {
        map = new HashMap();
        this.multiLevel = multiLevel;
    }

    /**
     * Construct from a string.
     * @throws IOException if the string is too short or invalid.
     */
    public SimpleFieldSet(String content, boolean multiLevel) throws IOException {
        map = new HashMap();
        this.multiLevel = multiLevel;
        StringReader sr = new StringReader(content);
        BufferedReader br = new BufferedReader(sr);
	    read(br);
    }

    /**
     * Construct from a string[].
     * @throws IOException if the string is too short or invalid.
     */
    public SimpleFieldSet(String[] content, boolean multiLevel) throws IOException {
        map = new HashMap();
        this.multiLevel = multiLevel;
        String content2=new String();
        for(int i=0;i<content.length;i++)
        	content2.concat(content[i]+";");
        StringReader sr = new StringReader(content2);
        BufferedReader br = new BufferedReader(sr);
	    read(br);
    }
    
    /**
     * Read from disk
     * Format:
     * blah=blah
     * blah=blah
     * End
     */
    private void read(BufferedReader br) throws IOException {
        boolean firstLine = true;
        while(true) {
            String line = br.readLine();
            if(line == null) {
                if(firstLine) throw new EOFException();
                throw new IOException();
            }
            firstLine = false;
            int index = line.indexOf('=');
            if(index >= 0) {
                // Mapping
                String before = line.substring(0, index);
                String after = line.substring(index+1);
                put(before, after);
            } else {
            	endMarker = line;
            	return;
            }
            
        }
    }

    /**
     * Read from disk
     * Format:
     * blah=blah
     * blah=blah
     * End
     */
    private void read(LineReader br, int maxLength, int bufferSize) throws IOException {
        boolean firstLine = true;
        while(true) {
            String line = br.readLine(maxLength, bufferSize);
            if(line == null) {
                if(firstLine) throw new EOFException();
                throw new IOException();
            }
            firstLine = false;
            int index = line.indexOf('=');
            if(index >= 0) {
                // Mapping
                String before = line.substring(0, index);
                String after = line.substring(index+1);
                put(before, after);
            } else {
            	endMarker = line;
            	return;
            }
            
        }
    }
    
    public String get(String key) {
    	if(multiLevel) {
    		int idx = key.indexOf(MULTI_LEVEL_CHAR);
    		if(idx == -1)
    			return (String) map.get(key);
    		else {
    			String before = key.substring(0, idx);
    			String after = key.substring(idx+1);
    			SimpleFieldSet fs = (SimpleFieldSet) (map.get(before));
    			if(fs == null) return null;
    			return fs.get(after);
    		}
    	} else {
    		return (String) map.get(key);
    	}
    }
    
    public String[] getAll(String key) {
    	return split(get(key));
    }

    private static final String[] split(String string) {
    	return string.split(";"); // slower???
//    	int index = string.indexOf(';');
//    	if(index == -1) return null;
//    	Vector v=new Vector();
//    	v.removeAllElements();
//        while(index>0){
//            // Mapping
//            String before = string.substring(0, index);         
//            String after = string.substring(index+1);
//            v.addElement(before);
//            string=after;
//            index = string.indexOf(';');
//        }
//    	
//    	return (String[]) v.toArray();
	}

	public void put(String key, String value) {
		int idx;
		if((!multiLevel) || (idx = key.indexOf(MULTI_LEVEL_CHAR)) == -1) {
			String x = (String) map.get(key);
			
			if(x == null) {
				map.put(key, value);
			} else {
				map.put(key, ((String)map.get(key))+";"+value);
			}
		} else {
			String before = key.substring(0, idx);
			String after = key.substring(idx+1);
			SimpleFieldSet fs = (SimpleFieldSet) (map.get(before));
			if(fs == null) {
				fs = new SimpleFieldSet(true);
				map.put(before, fs);
			}
			fs.put(after, value);
		}
    }

    /**
     * Write the contents of the SimpleFieldSet to a Writer.
     * @param osr
     */
	public void writeTo(Writer w) throws IOException {
		writeTo(w, "", false);
	}
	
    void writeTo(Writer w, String prefix, boolean noEndMarker) throws IOException {
        Set s = map.entrySet();
        Iterator i = s.iterator();
        for(;i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            Object v = entry.getValue();
            if(v instanceof String) {
                String value = (String) v;
                w.write(prefix+key+"="+value+"\n");
            } else {
            	SimpleFieldSet sfs = (SimpleFieldSet) v;
            	sfs.writeTo(w, prefix+key+MULTI_LEVEL_CHAR, true);
            }
        }
        if(!noEndMarker) {
        	if(endMarker != null)
        		w.write(endMarker+"\n");
        	else
        		w.write("End\n");
        }
    }
    
    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            writeTo(sw);
        } catch (IOException e) {
            Logger.error(this, "WTF?!: "+e+" in toString()!", e);
        }
        return sw.toString();
    }
    
    public String getEndMarker() {
    	return endMarker;
    }
    
    public void setEndMarker(String s) {
    	endMarker = s;
    }

	public SimpleFieldSet subset(String key) {
		if(!multiLevel)
			throw new IllegalArgumentException("Not multi-level!");
		int idx = key.indexOf(MULTI_LEVEL_CHAR);
		if(idx == -1)
			return (SimpleFieldSet) map.get(key);
		String before = key.substring(0, idx);
		String after = key.substring(idx+1);
		SimpleFieldSet fs = (SimpleFieldSet) map.get(before);
		if(fs == null) return null;
		return fs.subset(after);
	}

	public Iterator keyIterator() {
		return new KeyIterator();
	}
	
    public class KeyIterator implements Iterator {
    	
    	final Iterator mapIterator;
    	KeyIterator subIterator;
    	
    	public KeyIterator() {
    		mapIterator = map.keySet().iterator();
    	}

		public boolean hasNext() {
			if(subIterator != null && subIterator.hasNext()) return true;
			if(subIterator != null) subIterator = null;
			return mapIterator.hasNext();
		}

		public Object next() {
			while(true) { // tail-recurse so we get infinite loop instead of OOM in case of a loop...
				if(subIterator != null && subIterator.hasNext())
					return subIterator.next();
				if(subIterator != null) subIterator = null;
				if(mapIterator.hasNext()) {
					String key = (String) mapIterator.next();
					Object value = map.get(key);
					if(value instanceof String)
						return value;
					else {
						SimpleFieldSet fs = (SimpleFieldSet) value;
						subIterator = (KeyIterator) fs.keyIterator();
						continue;
					}
				}
				return null;
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public void put(String key, SimpleFieldSet fs) {
		if(!multiLevel)
			throw new IllegalArgumentException("Not multi-level");
		if(!fs.multiLevel)
			throw new IllegalArgumentException("Argument not multi-level");
		if(map.containsKey(key))
			throw new IllegalArgumentException("Already contains "+key+" but trying to add a SimpleFieldSet!");
		map.put(key, fs);
	}

}
