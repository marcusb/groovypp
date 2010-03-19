package shootout.meteor

@Typed
class MeteorGroovy {
	static final int X = 0;
	static final int Y = 1;
	static final int N_DIM = 2;

	static final int EVEN = 0;
	static final int ODD = 1;
	static final int N_PARITY = 2;

	static final int GOOD = 0;
	static final int BAD = 1;
	static final int ALWAYS_BAD = 2;

	static final int OPEN    = 0;
	static final int CLOSED  = 1;
	static final int N_FIXED = 2;

	static final int MAX_ISLAND_OFFSET = 1024;
	static final int N_COL = 5;
	static final int N_ROW = 10;
	static final int N_CELL = N_COL * N_ROW;
	static final int N_PIECE_TYPE = 10;
	static final int N_ORIENT = 12;

//-- Globals -------------------------
	static IslandInfo[] g_islandInfo = new IslandInfo [MAX_ISLAND_OFFSET];
	static int g_nIslandInfo = 0;
	static OkPieces[][] g_okPieces = new OkPieces [N_ROW][N_COL];

	static final int[] g_firstRegion = [
	    0x00, 0x01, 0x02, 0x03,   0x04, 0x01, 0x06, 0x07,
	    0x08, 0x01, 0x02, 0x03,   0x0c, 0x01, 0x0e, 0x0f,

	    0x10, 0x01, 0x02, 0x03,   0x04, 0x01, 0x06, 0x07,
	    0x18, 0x01, 0x02, 0x03,   0x1c, 0x01, 0x1e, 0x1f
	];

	static final int[] g_flip = [
	    0x00, 0x10, 0x08, 0x18, 0x04, 0x14, 0x0c, 0x1c,
	    0x02, 0x12, 0x0a, 0x1a, 0x06, 0x16, 0x0e, 0x1e,

	    0x01, 0x11, 0x09, 0x19, 0x05, 0x15, 0x0d, 0x1d,
	    0x03, 0x13, 0x0b, 0x1b, 0x07, 0x17, 0x0f, 0x1f,
	];

	static final int[] s_firstOne = [
	    0, 0, 1, 0,   2, 0, 1, 0,
	    3, 0, 1, 0,   2, 0, 1, 0,

	    4, 0, 1, 0,   2, 0, 1, 0,
	    3, 0, 1, 0,   2, 0, 1, 0,
	];

	static int getMask(int iPos) {
	    return (1 << (iPos));
	}

	static int floor(int top, int bot) {
	    int toZero = top / bot;
	    // negative numbers should be rounded down, not towards zero;
	    if ((toZero * bot != top) && ((top < 0) != (bot <= 0)))
	        toZero--;

	    return toZero;
	}

	static int getFirstOne(int v) {
	    int startPos = 0;
	    if (v == 0)
	        return 0;

	    int iPos = startPos;
	    int mask = 0xff << startPos;
	    while ((mask & v) == 0) {
	        mask <<= 8;
	        iPos += 8;
	    }
	    int result = (mask & v) >> iPos;
	    int resultLow = result & 0x0f;
	    if (resultLow != 0)
	        iPos += s_firstOne[resultLow];
	    else
	        iPos += 4 + s_firstOne[result >> 4];

	    return iPos;
	}

	static int countOnes(int v) {
	    int n = 0;
	    while (v != 0) {
	        n++;
	        v = v & (v - 1);
	    }

	    return n;
	}


	static int flipTwoRows(int bits) {
	    int flipped = MeteorGroovy.g_flip[bits >> MeteorGroovy.N_COL] << MeteorGroovy.N_COL;
	    return (flipped | MeteorGroovy.g_flip[bits & Board.TOP_ROW]);
	}

	static void markBad(IslandInfo info, int mask, int eo, boolean always) {
	    info.hasBad[eo][MeteorGroovy.OPEN] |= mask;
	    info.hasBad[eo][MeteorGroovy.CLOSED] |= mask;

	    if (always)
	        info.alwaysBad[eo] |= mask;
	}

	static void initGlobals() {
	    for (int i = 0; i < MeteorGroovy.MAX_ISLAND_OFFSET; i++)
	    {
	        MeteorGroovy.g_islandInfo[i] = new IslandInfo();
	    }

	    for (int i = 0; i < MeteorGroovy.N_ROW; i++)
	    {
	        for (int j = 0; j < MeteorGroovy.N_COL; j++)
	            MeteorGroovy.g_okPieces[i][j] = new OkPieces();
	    }
	}

//-- Classes -------------------------;

	static class OkPieces {
	    byte[] nPieces = new byte[MeteorGroovy.N_PIECE_TYPE];
	    int[][] pieceVec = new int[MeteorGroovy.N_PIECE_TYPE][MeteorGroovy.N_ORIENT];
	}


	static class IslandInfo {
	    int[][] hasBad  =  new int[MeteorGroovy.N_FIXED][MeteorGroovy.N_PARITY];
	    int[][] isKnown =  new int[MeteorGroovy.N_FIXED][MeteorGroovy.N_PARITY];
	    int[] alwaysBad =  new int[MeteorGroovy.N_PARITY];
	}
	
	static class Soln {
	    static final int NO_PIECE = -1;

	    boolean isEmpty() {
	        return (m_nPiece == 0);
	    }
	    void popPiece() {
	        m_nPiece--;
	        m_synched = false;
	    }
	    void pushPiece(int vec, int iPiece, int row) {
	        SPiece p = m_pieces[m_nPiece++];
	        p.vec = vec;
	        p.iPiece = (short) iPiece;
	        p.row = (short) row;
	    }

	    Soln() {
	        m_synched = false;
	        m_nPiece = 0;
	        init();
	    }

	    class SPiece {
	        int vec;
	        short iPiece;
	        short row;
	        SPiece() {}
	        SPiece(int avec, int apiece, int arow) {
	            vec = avec;
	            iPiece = (short)apiece;
	            row = (short)arow;
	        }
	        SPiece(SPiece other) {
	            vec = other.vec;
	            iPiece = other.iPiece;
	            row = other.row;
	        }
	    }

	    SPiece[] m_pieces = new SPiece [MeteorGroovy.N_PIECE_TYPE];
	    int m_nPiece;
	    byte[][] m_cells = new byte [MeteorGroovy.N_ROW][MeteorGroovy.N_COL];
	    boolean m_synched;

	    void init() {
	        for (int i = 0; i < MeteorGroovy.N_PIECE_TYPE; i++)
	            m_pieces[i] = new SPiece();
	    }
	    Soln (int fillVal) {
	        init();
	        m_nPiece = 0;
	        fill(fillVal);
	    }
	    public Soln clone2() {
	        Soln s = new Soln();
	        for (int i = 0; i < m_pieces.length; i++)
	            s.m_pieces[i] = new SPiece(m_pieces[i]);

	        s.m_nPiece = m_nPiece;
	        //System.arraycopy(m_cells, 0, s.m_cells, 0, N_CELL);
	        for (int i = 0; i < MeteorGroovy.N_ROW; i++)
	        {
	            for (int j = 0; j < MeteorGroovy.N_COL; j ++)
	            {
	                s.m_cells[i][j] = m_cells[i][j];
	            }
	        }

	        s.m_synched = m_synched;
	        return s;
	    }

	    void fill(int val) {
	        m_synched = false;
	        for (int i = 0; i < MeteorGroovy.N_ROW; i++)
	        {
	            for (int j = 0; j < MeteorGroovy.N_COL; j++)
	                m_cells[i][j] = (byte) val;
	        }
	    }

	    public String toString()  {
	        StringBuffer result = new StringBuffer(MeteorGroovy.N_CELL * 2);

	        for (int y = 0; y < MeteorGroovy.N_ROW; y++) {
	            for (int x = 0; x < MeteorGroovy.N_COL; x++) {
	                int val = m_cells[y][x];
	                //if (val == NO_PIECE) result.append('.');
	                result.append(val);
	                result.append(' ');
	            }
	            result.append('\n');

	            // indent every second line
	            if (y % 2 == 0)
	                result.append(" ");
	        }
	        return result.toString();
	    }

	    void setCells() {
	        if (m_synched)
	            return;

	        for (int iPiece = 0; iPiece < m_nPiece; iPiece++) {
	            SPiece p = m_pieces[iPiece];
	            int vec = p.vec;
	            byte pID = (byte) p.iPiece;
	            int rowOffset = p.row;

	            int nNewCells = 0;
	            for (int y = rowOffset; y < MeteorGroovy.N_ROW; y++) {
	                for (int x = 0; x < MeteorGroovy.N_COL; x++) {
	                    if ((vec & 1) != 0) {
	                        m_cells[y][x] = pID;
	                        nNewCells++;
	                    }
	                    vec >>= 1;
	                }
	                if (nNewCells == Piece.N_ELEM)
	                    break;
	            }
	        }
	        m_synched = true;
	    }

	    boolean lessThan(Soln r) {
	        if (m_pieces[0].iPiece != r.m_pieces[0].iPiece) {
	            return m_pieces[0].iPiece < r.m_pieces[0].iPiece;
	        }

	        setCells();
	        r.setCells();

	        for (int y = 0; y < MeteorGroovy.N_ROW; y++) {
	            for (int x = 0; x < MeteorGroovy.N_COL; x++) {
	                int lval = m_cells[y][x];
	                int rval = r.m_cells[y][x];

	                if (lval != rval)
	                    return (lval < rval);
	            }
	        }

	        return false;
	    }

	    void spin(Soln spun) {
	        setCells();

	        for (int y = 0; y < MeteorGroovy.N_ROW; y++) {
	            for (int x = 0; x < MeteorGroovy.N_COL; x++) {
	                byte flipped = m_cells[MeteorGroovy.N_ROW - y - 1][MeteorGroovy.N_COL - x - 1];
	                spun.m_cells[y][x] = flipped;
	            }
	        }


	        spun.m_pieces[0].iPiece = m_pieces[MeteorGroovy.N_PIECE_TYPE - 1].iPiece;
	        spun.m_synched = true;
	    }
	}


//-----------------------
	static class Board {
	    static final int L_EDGE_MASK =
	                                   ((1 <<  0) | (1 <<  5) | (1 << 10) | (1 << 15) |
	                                    (1 << 20) | (1 << 25) | (1 << 30));
	    static final int R_EDGE_MASK = L_EDGE_MASK << 4;
	    static final int TOP_ROW = (1 << MeteorGroovy.N_COL) - 1;
	    static final int ROW_0_MASK =
	        TOP_ROW | (TOP_ROW << 10) | (TOP_ROW << 20) | (TOP_ROW << 30);
	    static final int ROW_1_MASK = ROW_0_MASK << 5;
	    static final int BOARD_MASK = (1 << 30) - 1;

	    static int getIndex(int x, int y) {
	        return y * MeteorGroovy.N_COL + x;
	    }

	    Soln m_curSoln;
	    Soln m_minSoln;
	    Soln m_maxSoln;
	    int m_nSoln;

	    Board () {
	        m_curSoln = new Soln(Soln.NO_PIECE);
	        m_minSoln = new Soln(MeteorGroovy.N_PIECE_TYPE);
	        m_maxSoln = new Soln(Soln.NO_PIECE);
	        m_nSoln = (0);
	    }

	    static boolean badRegion(int[] toFill, int rNew)
	    {
	        // grow empty region, until it doesn't change any more;
	        int region;
	        while(true) {
	            region = rNew;

	            // simple grow up/down
	            rNew |= (region >> MeteorGroovy.N_COL);
	            rNew |= (region << MeteorGroovy.N_COL);

	            // grow right/left
	            rNew |= (region & ~L_EDGE_MASK) >> 1;
	            rNew |= (region & ~R_EDGE_MASK) << 1;

	            // tricky growth
	            int evenRegion = region & (ROW_0_MASK & ~L_EDGE_MASK);
	            rNew |= evenRegion >> (MeteorGroovy.N_COL + 1);
	            rNew |= evenRegion << (MeteorGroovy.N_COL - 1);
	            int oddRegion = region & (ROW_1_MASK & ~R_EDGE_MASK);
	            rNew |= oddRegion >> (MeteorGroovy.N_COL - 1);
	            rNew |= oddRegion << (MeteorGroovy.N_COL + 1);

	            // clamp against existing pieces
	            rNew &= toFill[0];
		        boolean cond = (rNew != toFill[0]) && (rNew != region);
		        if (!cond)
		            break;
	        }

	        // subtract empty region from board
	        toFill[0] ^= rNew;

	        int nCells = MeteorGroovy.countOnes(toFill[0]);
	        return (nCells % Piece.N_ELEM != 0);
	    }

	    static int hasBadIslands(int boardVec, int row)
	    {
	        // skip over any filled rows
	        while ((boardVec & TOP_ROW) == TOP_ROW) {
	            boardVec >>= MeteorGroovy.N_COL;
	            row++;
	        }

	        int iInfo = boardVec & ((1 << 2 * MeteorGroovy.N_COL) - 1);
	        IslandInfo info = MeteorGroovy.g_islandInfo[iInfo];

	        int lastRow = (boardVec >> (2 * MeteorGroovy.N_COL)) & TOP_ROW;
	        int mask = MeteorGroovy.getMask(lastRow);
	        int isOdd = row & 1;

	        if ((info.alwaysBad[isOdd] & mask) != 0)
	            return MeteorGroovy.BAD;

	        if ((boardVec & (TOP_ROW << MeteorGroovy.N_COL * 3)) != 0)
	            return calcBadIslands(boardVec, row);

	        int isClosed = (row > 6) ? 1 : 0;

	        if ((info.isKnown[isOdd][isClosed] & mask) != 0)
	            return (info.hasBad[isOdd][isClosed] & mask);

	        if (boardVec == 0)
	            return MeteorGroovy.GOOD;

	        int hasBad = calcBadIslands(boardVec, row);

	        info.isKnown[isOdd][isClosed] |= mask;
	        if (hasBad != 0)
	            info.hasBad[isOdd][isClosed] |= mask;

	        return hasBad;
	    }
	    static int calcBadIslands(int boardVec, int row)
	    {
	        int[] toFill = [~boardVec];
	        if ((row & 1) != 0) {
	            row--;
	            toFill[0] <<= MeteorGroovy.N_COL;
	        }

	        int boardMask = BOARD_MASK;
	        if (row > 4) {
	            int boardMaskShift = (row - 4) * MeteorGroovy.N_COL;
	            boardMask >>= boardMaskShift;
	        }
	        toFill[0] &= boardMask;

	        // a little pre-work to speed things up
	        int bottom = (TOP_ROW << (5 * MeteorGroovy.N_COL));
	        boolean filled = ((bottom & toFill[0]) == bottom);
	        while ((bottom & toFill[0]) == bottom) {
	            toFill[0] ^= bottom;
	            bottom >>= MeteorGroovy.N_COL;
	        }

	        int startRegion;
	        if (filled || (row < 4))
	            startRegion = bottom & toFill[0];
	        else {
	            startRegion = MeteorGroovy.g_firstRegion[toFill[0] & TOP_ROW];
	            if (startRegion == 0)  {
	                startRegion = (toFill[0] >> MeteorGroovy.N_COL) & TOP_ROW;
	                startRegion = MeteorGroovy.g_firstRegion[startRegion];
	                startRegion <<= MeteorGroovy.N_COL;
	            }
	            startRegion |= (startRegion << MeteorGroovy.N_COL) & toFill[0];
	        }

	        while (toFill[0] != 0)    {
	            if (badRegion(toFill, startRegion))
	                return ((toFill[0]!=0) ? MeteorGroovy.ALWAYS_BAD : MeteorGroovy.BAD);
	            int iPos = MeteorGroovy.getFirstOne(toFill[0]);
	            startRegion = MeteorGroovy.getMask(iPos);
	        }

	        return MeteorGroovy.GOOD;
	    }
	    static void calcAlwaysBad() {
	        for (int iWord = 1; iWord < MeteorGroovy.MAX_ISLAND_OFFSET; iWord++) {
	            IslandInfo isleInfo = MeteorGroovy.g_islandInfo[iWord];
	            IslandInfo flipped = MeteorGroovy.g_islandInfo[MeteorGroovy.flipTwoRows(iWord)];

		        int mask = 1;
	            for (int i = 0; i < 32; i++) {
	                int boardVec = (i << (2 * MeteorGroovy.N_COL)) | iWord;
	                if ((isleInfo.isKnown[0][MeteorGroovy.OPEN] & mask) != 0) {
		                mask = mask << 1
	                    continue;
		            }

	                int hasBad = calcBadIslands(boardVec, 0);
	                if (hasBad != MeteorGroovy.GOOD) {
	                    boolean always = (hasBad==MeteorGroovy.ALWAYS_BAD);
	                    MeteorGroovy.markBad(isleInfo, mask, MeteorGroovy.EVEN, always);

	                    int flipMask = MeteorGroovy.getMask(MeteorGroovy.g_flip[i]);
	                    MeteorGroovy.markBad(flipped, flipMask, MeteorGroovy.ODD, always);
	                }
		            mask = mask << 1;
	            }
	            flipped.isKnown[1][MeteorGroovy.OPEN] =  -1;
	            isleInfo.isKnown[0][MeteorGroovy.OPEN] = -1;
	        }
	    }

	    static boolean hasBadIslandsSingle(int boardVec, int row)
	    {
	        int[] toFill = [~boardVec];
	        boolean isOdd = ((row & 1) != 0);
	        if (isOdd) {
	            row--;
	            toFill[0] <<= MeteorGroovy.N_COL; // shift to even aligned
	            toFill[0] |= TOP_ROW;
	        }

	        int startRegion = TOP_ROW;
	        int lastRow = TOP_ROW << (5 * MeteorGroovy.N_COL);
	        int boardMask = BOARD_MASK; // all but the first two bits
	        if (row >= 4)
	            boardMask >>= ((row - 4) * MeteorGroovy.N_COL);
	        else if (isOdd || (row == 0))
	            startRegion = lastRow;

	        toFill[0] &= boardMask;
	        startRegion &= toFill[0];

	        while (toFill[0] != 0)    {
	            if (badRegion(toFill, startRegion))
	                return true;
	            int iPos = MeteorGroovy.getFirstOne(toFill[0]);
	            startRegion = MeteorGroovy.getMask(iPos);
	        }

	        return false;
	    }

	    void genAllSolutions(int boardVec, int placedPieces, int row)
	    {
	        while ((boardVec & TOP_ROW) == TOP_ROW) {
	            boardVec >>= MeteorGroovy.N_COL;
	            row++;
	        }
	        int iNextFill = s_firstOne[~boardVec & TOP_ROW];
	        OkPieces allowed = MeteorGroovy.g_okPieces[row][iNextFill];

	        int iPiece = MeteorGroovy.getFirstOne(~placedPieces);
	        int pieceMask = MeteorGroovy.getMask(iPiece);
	        for (; iPiece < MeteorGroovy.N_PIECE_TYPE; iPiece++)
	        {
	            if ((pieceMask & placedPieces) != 0) {
		            pieceMask <<= 1
	                continue;
		        }

	            placedPieces |= pieceMask;
	            for (int iOrient = 0; iOrient < allowed.nPieces[iPiece]; iOrient++) {
	                int pieceVec = allowed.pieceVec[iPiece][iOrient];

	                if ((pieceVec & boardVec) != 0)
	                    continue;

	                boardVec |= pieceVec;

	                if ((hasBadIslands(boardVec, row)) != 0) {
	                    boardVec ^= pieceVec;
	                    continue;
	                }

	                m_curSoln.pushPiece(pieceVec, iPiece, row);

	                // recur or record solution
	                if (placedPieces != Piece.ALL_PIECE_MASK)
	                    genAllSolutions(boardVec, placedPieces, row);
	                else
	                    recordSolution(m_curSoln);

	                boardVec ^= pieceVec;
	                m_curSoln.popPiece();
	            }

	            placedPieces ^= pieceMask;
		        pieceMask <<= 1
	        }
	    }

	    void recordSolution(Soln s) {
	        m_nSoln += 2;

	        if (m_minSoln.isEmpty()) {
	            m_minSoln = m_maxSoln = s.clone2();
	            return;
	        }

	        if (s.lessThan(m_minSoln))
	            m_minSoln = s.clone2();
	        else if (m_maxSoln.lessThan(s))
	            m_maxSoln = s.clone2();

	        Soln spun = new Soln();
	        s.spin(spun);
	        if (spun.lessThan(m_minSoln))
	            m_minSoln = spun;
	        else if (m_maxSoln.lessThan(spun))
	            m_maxSoln = spun;
	    }
	}

//----------------------
	static class Piece {
	    class Instance {
	        long m_allowed;
	        int m_vec;
	        int m_offset;
	    }

	    static final int N_ELEM = 5;
	    static final int ALL_PIECE_MASK = (1 << MeteorGroovy.N_PIECE_TYPE) - 1;
	    static final int SKIP_PIECE = 5;

	    static final int[] BaseVecs = [
	        0x10f, 0x0cb, 0x1087, 0x427, 0x465,
	        0x0c7, 0x8423, 0x0a7, 0x187, 0x08f
	    ];

	    static Piece[][] s_basePiece = new Piece [MeteorGroovy.N_PIECE_TYPE][MeteorGroovy.N_ORIENT];

	    Instance[] m_instance = new Instance [MeteorGroovy.N_PARITY];

	    void init() {
	        for (int i = 0; i < MeteorGroovy.N_PARITY; i++)
	            m_instance[i] = new Instance();
	    }
	    Piece() {
	        init();
	    }

	    static {
	        for (int i = 0; i < MeteorGroovy.N_PIECE_TYPE; i++) {
	            for (int j = 0; j < MeteorGroovy.N_ORIENT; j++)
	                s_basePiece[i][j] = new Piece();
	        }
	    }
	    static void setCoordList(int vec, int[][] pts) {
	        int iPt = 0;
	        int mask = 1;
	        for (int y = 0; y < MeteorGroovy.N_ROW; y++) {
	            for (int x = 0; x < MeteorGroovy.N_COL; x++) {
	                if ((mask & vec) != 0) {
	                    pts[iPt][MeteorGroovy.X] = x;
	                    pts[iPt][MeteorGroovy.Y] = y;

	                    iPt++;
	                }
	                mask <<= 1;
	            }
	        }
	    }

	    static int toBitVector(int[][] pts) {
	        int y, x;
	        int result = 0;
	        for (int iPt = 0; iPt < N_ELEM; iPt++) {
	            x = pts[iPt][MeteorGroovy.X];
	            y = pts[iPt][MeteorGroovy.Y];

	            int pos = Board.getIndex(x, y);
	            result |= (1 << pos);
	        }

	        return result;
	    }

	    static void shiftUpLines(int[][] pts, int shift) {

	        for (int iPt = 0; iPt < N_ELEM; iPt++) {
	            if ((pts[iPt][MeteorGroovy.Y] & shift & 0x1) != 0)
	                (pts[iPt][MeteorGroovy.X])++;
	            pts[iPt][MeteorGroovy.Y] -= shift;
	        }
	    }

	    static int shiftToX0(int[][] pts, Instance instance, int offsetRow)
	    {
	        int x, y, iPt;
	        int xMin = pts[0][MeteorGroovy.X];
	        int xMax = xMin;
	        for (iPt = 1; iPt < N_ELEM; iPt++) {
	            x = pts[iPt][MeteorGroovy.X];
	            y = pts[iPt][MeteorGroovy.Y];

	            if (x < xMin)
	                xMin = x;
	            else if (x > xMax)
	                xMax = x;
	        }

	        int offset = N_ELEM;
	        for (iPt = 0; iPt < N_ELEM; iPt++) {

	            pts[iPt][MeteorGroovy.X] -= xMin;

	            if ((pts[iPt][MeteorGroovy.Y] == offsetRow) && (pts[iPt][MeteorGroovy.X] < offset))
	                offset = pts[iPt][MeteorGroovy.X];
	        }

	        instance.m_offset = offset;
	        instance.m_vec = toBitVector(pts);
	        return xMax - xMin;
	    }

	    public void setOkPos(int isOdd, int w, int h) {
	        Instance p = m_instance[isOdd];
	        p.m_allowed = 0;
	        long posMask = 1L << (isOdd * MeteorGroovy.N_COL);

	        for (int y = isOdd; y < MeteorGroovy.N_ROW - h; y+=2) {
	            if ((p.m_offset) != 0)
	                posMask <<= p.m_offset;

	            for (int xPos = 0; xPos < MeteorGroovy.N_COL - p.m_offset; xPos++) {

	                if (xPos >= MeteorGroovy.N_COL - w) {
		                posMask <<= 1
	                    continue;
	                }

	                int pieceVec = p.m_vec << xPos;

	                if (Board.hasBadIslandsSingle(pieceVec, y)) {
		                posMask <<= 1
	                    continue;
	                }

	                p.m_allowed |= posMask;
		            posMask <<= 1
	            }
		        posMask <<= MeteorGroovy.N_COL;
	        }
	    }

	    static void genOrientation(int vec, int iOrient, Piece target)
	    {
	        int[][] pts = new int[N_ELEM][MeteorGroovy.N_DIM];
	        setCoordList(vec, pts);

	        int y, x, iPt;
	        int rot = iOrient % 6;
	        int flip = iOrient >= 6 ? 1 : 0;
	        if (flip != 0) {
	            for (iPt = 0; iPt < N_ELEM; iPt++)
	                pts[iPt][MeteorGroovy.Y] = -pts[iPt][MeteorGroovy.Y];
	        }

	        while ((rot--) != 0) {
	            for (iPt = 0; iPt < N_ELEM; iPt++) {
	                x = pts[iPt][MeteorGroovy.X];
	                y = pts[iPt][MeteorGroovy.Y];

	                int xNew = MeteorGroovy.floor((2 * x - 3 * y + 1), 4);
	                int yNew = MeteorGroovy.floor((2 * x + y + 1), 2);
	                pts[iPt][MeteorGroovy.X] = xNew;
	                pts[iPt][MeteorGroovy.Y] = yNew;
	            }
	        }

	        int yMin = pts[0][MeteorGroovy.Y];
	        int yMax = yMin;
	        for (iPt = 1; iPt < N_ELEM; iPt++) {
	            y = pts[iPt][MeteorGroovy.Y];

	            if (y < yMin)
	                yMin = y;
	            else if (y > yMax)
	                yMax = y;
	        }
	        int h = yMax - yMin;
	        Instance even = target.m_instance[MeteorGroovy.EVEN];
	        Instance odd = target.m_instance[MeteorGroovy.ODD];

	        shiftUpLines(pts, yMin);
	        int w = shiftToX0(pts, even, 0);
	        target.setOkPos(MeteorGroovy.EVEN, w, h);
	        even.m_vec >>= even.m_offset;

	        shiftUpLines(pts, -1);
	        w = shiftToX0(pts, odd, 1);
	        odd.m_vec >>= MeteorGroovy.N_COL;
	        target.setOkPos(MeteorGroovy.ODD, w, h);
	        odd.m_vec >>= odd.m_offset;
	    }

	    static void genAllOrientations() {
	        for (int iPiece = 0; iPiece < MeteorGroovy.N_PIECE_TYPE; iPiece++) {
	            int refPiece = BaseVecs[iPiece];
	            for (int iOrient = 0; iOrient < MeteorGroovy.N_ORIENT; iOrient++) {
	                Piece p = s_basePiece[iPiece][iOrient];
	                genOrientation(refPiece, iOrient, p);
		            // ??
	                if ((iPiece == SKIP_PIECE)  && ((((int)(iOrient / 3)) & 1) != 0))
	                    p.m_instance[0].m_allowed = p.m_instance[1].m_allowed = 0;
	            }
	        }
	        for (int iPiece = 0; iPiece < MeteorGroovy.N_PIECE_TYPE; iPiece++) {
	            for (int iOrient = 0; iOrient < MeteorGroovy.N_ORIENT; iOrient++) {
	                long mask = 1;
	                for (int iRow = 0; iRow < MeteorGroovy.N_ROW; iRow++) {
	                    Instance p = getPiece(iPiece, iOrient, (iRow & 1));
	                    for (int iCol = 0; iCol < MeteorGroovy.N_COL; iCol++) {
	                        OkPieces allowed = MeteorGroovy.g_okPieces[iRow][iCol];
	                        if ((p.m_allowed & mask) != 0) {
	                            allowed.pieceVec[iPiece][allowed.nPieces[iPiece]] = p.m_vec << iCol;
	                            (allowed.nPieces[iPiece])++;
	                        }

	                        mask <<= 1;
	                    }
	                }
	            }
	        }
	    }

	    static Instance getPiece(int iPiece, int iOrient, int iParity) {
	        return s_basePiece[iPiece][iOrient].m_instance[iParity];
	    }
	}
	
//-- Main ---------------------------
	public static void main(String[] args) {
	    if (args.length > 2)
	        System.exit(-1); // spec says this is an error;

		def start = System.currentTimeMillis();

	    initGlobals();
	    Board b = new Board();
	    Piece.genAllOrientations();
	    Board.calcAlwaysBad();
	    b.genAllSolutions(0, 0, 0);

	    System.out.println(b.m_nSoln + " solutions found\n");
	    System.out.println(b.m_minSoln);
	    System.out.println(b.m_maxSoln);

		def total = System.currentTimeMillis() - start;
		println "[Meteor-Groovy Benchmark Result: $total ]"
	}

}
