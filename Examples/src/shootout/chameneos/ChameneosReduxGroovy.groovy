package shootout.chameneos

@Typed
class ChameneosReduxGroovy {

	enum Colour {
	    blue,
	    red,
	    yellow
	}

	private static Colour doCompliment(Colour c1, Colour c2) {
	    switch (c1) {
	    case Colour.blue:
	        switch (c2) {
	        case Colour.blue:
	            return Colour.blue
	        case Colour.red:
	            return Colour.yellow
	        case Colour.yellow:
	            return Colour.red
	        }
	    case Colour.red:
	        switch (c2) {
	        case Colour.blue:
	            return Colour.yellow
	        case Colour.red:
	            return Colour.red
	        case Colour.yellow:
	            return Colour.blue
	        }
	    case Colour.yellow:
	        switch (c2) {
	        case Colour.blue:
	            return Colour.red
	        case Colour.red:
	            return Colour.blue
	        case Colour.yellow:
	            return Colour.yellow
	        }
	    }

	    throw new RuntimeException("Error")
	}

	static class MeetingPlace {

	    private int meetingsLeft

	    public MeetingPlace(int meetings) {
	        this.meetingsLeft = meetings
	    }

	    private Colour firstColour = null
	    private int firstId = 0
	    Future<Pair> current

	    Pair meet(int id, Colour c) throws Exception {
	        Future<Pair> newPair
	        synchronized (this) {
	            if (meetingsLeft == 0) {
	                throw new Exception("Finished")
	            } else {
	                if (firstColour == null) {
	                    firstColour = c
	                    firstId = id
	                    current = new Future<Pair>()
	                } else {
	                    Colour newColour = ChameneosReduxGroovy.doCompliment(c, firstColour)
	                    current.setItem(new Pair(id == firstId, newColour))
	                    firstColour = null
	                    meetingsLeft--
	                }
	                newPair = current
	            }
	        }
	        newPair.getItem()

	    }
	}

	public static class Future<T> {

	    private volatile T t

	    public T getItem() {
	        while (t == null) {
	            Thread.yield()
	        }
	        return t
	    }

	    // no synchronization necessary as assignment is atomic
	    public void setItem(T t) {
	        this.t = t
	    }
	}
	

	static class Creature implements Runnable {

	    private final MeetingPlace place
	    private int count = 0
	    private int sameCount = 0
	    private Colour colour
	    private int id

	    public Creature(MeetingPlace place, Colour colour) {
	        this.place = place
	        this.id = System.identityHashCode(this)
	        this.colour = colour
	    }

	    public void run() {
	        try {

	            while (true) {
	                Pair p = place.meet(id, colour)
	                colour = p.colour
	                if (p.sameId) {
	                    sameCount++
	                }
	                count++
	            }

	        } catch (Exception e) {}
	    }

	    public int getCount() {
	        return count;
	    }

	    public String toString() {
	        return String.valueOf(count) + ChameneosReduxGroovy.getNumber(sameCount);
	    }
	}

	private static void run(int n, boolean isWarm, Colour[] colours) {
	    MeetingPlace place = new MeetingPlace(n)
	    Creature[] creatures = new Creature[colours.length]

	    for (int i = 0; i < colours.length; i++) {
	        if (isWarm)
		        print " " + colours[i]
	        creatures[i] = new Creature(place, colours[i])
	    }
	    if (isWarm)
		    println ""
	    Thread[] ts = new Thread[colours.length]
	    for (int i = 0; i < colours.length; i++) {
	        ts[i] = new Thread(creatures[i])
	        ts[i].start()
	    }

	    for (Thread t : ts) {
	        try {
	            t.join()
	        } catch (InterruptedException e) {
	        }
	    }

	    int total = 0
	    for (Creature creature : creatures) {
	        if (isWarm)
		        println creature;
	        total += creature.getCount()
	    }
	    if (isWarm)
		    println getNumber(total)

	    if (isWarm)
		    println ""
	}

	public static void main(String[] args){
		def start = System.currentTimeMillis()
	    for (int i=0; i<65; ++i)
		    ChameneosRedux.program_main(args,false)
	    ChameneosRedux.program_main(args,true)
		def total = System.currentTimeMillis() - start
		println "[Chameneos Redux-Groovy Benchmark Result: $total ]"
	}

	public static void program_main(String[] args, boolean isWarm) {

	    int n = 600
		if (args.length > 0)
			n = Integer.parseInt(args[0])

	    if (isWarm){
	       printColours()
	       System.out.println()
	    }
	    run(n, isWarm, Colour.blue, Colour.red, Colour.yellow)
	    run(n, isWarm, Colour.blue, Colour.red, Colour.yellow, Colour.red, Colour.yellow,
	            Colour.blue, Colour.red, Colour.yellow, Colour.red, Colour.blue)
	}

	public static class Pair {
	    public final boolean sameId
	    public final Colour colour

	    public Pair(boolean sameId, Colour c) {
	        this.sameId = sameId
	        this.colour = c
	    }
	}

	private static final String[] NUMBERS = [
	    "zero", "one", "two", "three", "four", "five",
	    "six", "seven", "eight", "nine"]

	private static String getNumber(int n) {
	    StringBuilder sb = new StringBuilder()
	    String nStr = String.valueOf(n)
	    for (int i = 0; i < nStr.length(); i++) {
	        sb.append(" ")
	        sb.append(NUMBERS[Character.getNumericValue(nStr.charAt(i))])
	    }

	    return sb.toString()
	}

	private static void printColours() {
	    printColours(Colour.blue, Colour.blue)
	    printColours(Colour.blue, Colour.red)
	    printColours(Colour.blue, Colour.yellow)
	    printColours(Colour.red, Colour.blue)
	    printColours(Colour.red, Colour.red)
	    printColours(Colour.red, Colour.yellow)
	    printColours(Colour.yellow, Colour.blue)
	    printColours(Colour.yellow, Colour.red)
	    printColours(Colour.yellow, Colour.yellow)
	}

	private static void printColours(Colour c1, Colour c2) {
	    System.out.println(c1.toString() + " + " + c2.toString() + " -> " + doCompliment(c1, c2).toString());
	}
	
	
}
