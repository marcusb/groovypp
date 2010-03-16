package shootout.revcomp

@Typed
class RevCompGroovy {
	static final byte[] cmp = new byte[128]

	static {
		for (int i = 0; i < cmp.length; i++) cmp[i] = (byte) i
		cmp[(char)'t'] = cmp[(char)'T'] = (char)'A'
		cmp[(char)'a'] = cmp[(char)'A'] = (char)'T'
		cmp[(char)'g'] = cmp[(char)'G'] = (char)'C'
		cmp[(char)'c'] = cmp[(char)'C'] = (char)'G'
		cmp[(char)'v'] = cmp[(char)'V'] = (char)'B'
		cmp[(char)'h'] = cmp[(char)'H'] = (char)'D'
		cmp[(char)'r'] = cmp[(char)'R'] = (char)'Y'
		cmp[(char)'m'] = cmp[(char)'M'] = (char)'K'
		cmp[(char)'y'] = cmp[(char)'Y'] = (char)'R'
		cmp[(char)'k'] = cmp[(char)'K'] = (char)'M'
		cmp[(char)'b'] = cmp[(char)'B'] = (char)'V'
		cmp[(char)'d'] = cmp[(char)'D'] = (char)'H'
		cmp[(char)'u'] = cmp[(char)'U'] = (char)'A'
	}

	static class ReversibleByteArray extends java.io.ByteArrayOutputStream {
		void reverse() throws Exception {
			if (count > 0) {
				int begin = 0, end = count - 1
				while (buf[begin++] != '\n') {}
				while (begin <= end) {
					if (buf[begin] == '\n') begin++
					if (buf[end] == '\n') end--
					if (begin <= end) {
						byte tmp = buf[begin]
						buf[begin++] = cmp[buf[end]]
						buf[end--] = cmp[tmp]
					}
				}
//				System.out.write(buf, 0, count)
			}
		}
	}

	static void main(String[] args) throws Exception {
		def start = System.currentTimeMillis();
		byte[] line = new byte[82]
		int read
		ReversibleByteArray buf = new ReversibleByteArray()
		while ((read = System.in.read(line)) != -1) {
			int i = 0, last = 0
			while (i < read) {
				if (line[i] == '>') {
					buf.write(line, last, i - last)
					buf.reverse()
					buf.reset()
					last = i
				}
				i++
			}
			buf.write(line, last, read - last)
		}
		buf.reverse()
		def total = System.currentTimeMillis() - start
		println "[Revcomp-Groovy Benchmark Result: $total ]"
	}
}