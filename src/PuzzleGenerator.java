import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class PuzzleGenerator {
    final char nullChar = '@';
    private String[] words;
    private int height;
    private int width;
    private char[][] puzzle;
    private char[][] answer;
    private ArrayList<Pos> schemes;
    public PuzzleGenerator(int h, int w, String[] words) throws Exception {
        this.height = h;
        this.width = w;
        // format: puzzle[row][col]
        this.puzzle = new char[height][width];
        this.answer = new char[height][width];
        for (int row=0; row<height; row++) {
            for (int col = 0; col < width; col++) {
                // initialize all to nullChar.
                puzzle[row][col] = nullChar;
                answer[row][col] = nullChar;
            }
        }
        // store everything in upper case
        this.words = new String[words.length];
        for (int i=0; i<words.length; i++) {
            this.words[i] = words[i].toUpperCase();
        }
        // get all schemes of layout
        schemes = new ArrayList<>(); // these are used only to get new instance of Pos.
        schemes.add(new HorizontalPos(0,0));
        schemes.add(new VerticalPos(0,0));
        schemes.add(new LeftBottomRightTopPos(0,0));
        schemes.add(new LeftTopRightBottom(0,0));

        // schemes.add(new ....Pos(0,0));

        generatePuzzle();
    }
    private void generatePuzzle() {
        for (String word : words) {
            fillWord(word);
        }
        fillEmptySpots();
    }
    private void fillEmptySpots() {
        for (int row=0; row<height; row++) {
            for (int col = 0; col < width; col++) {
                if (puzzle[row][col] == nullChar) {
                    // for every empty spot
                    // fill in answer
                    answer[row][col] = '-';
                    // check repeatedly until correct letter is found
                    while (true)
                    {
                        char charToFillIn = getRandomLetter(1);
                        Iterator schemeIt = schemes.iterator();
                        Pos currentScheme = null;
                        boolean valid = true; // assume letter is valid
                        // Check with every scheme
                        while ((currentScheme = schemeIt.hasNext() ? ((Pos)schemeIt.next()).getCopy() : null) != null) {
                            currentScheme.reset(row,col);
                            if (charValidAt(charToFillIn, currentScheme)) continue; // continue checking if valid
                            else {
                                valid = false;
                                StdOut.println(String.format("Letter %c failed at (%d, %d) for scheme %s, changing to another letter..", charToFillIn, row, col, currentScheme.getClass().getName()));
                                break;
                            }
                        }
                        if (valid) // if passed every scheme
                        {
                            puzzle[row][col] = charToFillIn; // set it to the current valid letter.
                            break;
                        }
                    }

                }
            }
        }
    }
    // a helper method to check if filling a char at given Pos is legal, using the given scheme
    private boolean charValidAt(char charToFill, Pos posToFill) {
        // check against every word given
        for (String queryWord: words) {
            /* goal: For a queryWord 'MERRY' and a fullString, produce a corresponding rangeString in the puzzle that looks like:
            fullString:  .._ _ _ _ _ _[ ]_ _ _ _ _ _ _.....
                                                 |
                               | | | |   | | | | |
                               v v v v   v v v v |
            rangeString:       _ _ _ _ x _ _ _ _ |
                                                 |
                              (M E R R Y _ _ _ _)|
                              (_ M E R R Y _ _ _)|
                              (_ _ M E R R Y _ _)|
                              (_ _ _ M E R R Y _)|
                              (_ _ _ _ M E R R Y)|
                               |                 |
                          startIndex          endIndex
                            Where
                            _ represents random letter,
                            [ ] represents the position of posToFill, whose value should be nullChar (unset)
                            x represents the character to be filled in
                            (...) represents the only scenarios of replicating the query word.
             */
            String fullString = posToFill.getString();
            int startIndex = posToFill.getIndexInString() - queryWord.length() + 1;
            int endIndex = posToFill.getIndexInString() + queryWord.length();
            // cut the bounds
            startIndex = startIndex<0 ? 0 : startIndex;
            endIndex = endIndex>fullString.length()-1 ? fullString.length()-1: endIndex;

            StringBuilder rangeStringBuilder = new StringBuilder(fullString);
            rangeStringBuilder.setCharAt(posToFill.getIndexInString(), charToFill);
            String rangeString = rangeStringBuilder.substring(startIndex, endIndex);
            // test
            // StdOut.println(String.format("For queryWord '%s' and \nfullString '%s' \nrangeString is '%s'", queryWord, fullString, rangeString));
            if (rangeString.contains(queryWord)) return false;
        }
        return true;
    }

    // return a random upper case letter
    // heuristic argument range from 0 to 1
    // 1 means always return letters from the set of words
    // 0 means get random letter
    private char getRandomLetter(double heuristic) {
        double cutOff = StdRandom.uniform();
        if (cutOff < heuristic) {
            // return a character present in the current words
            String luckyWord = words[StdRandom.uniform(words.length)];
            char luckyChar = luckyWord.charAt(StdRandom.uniform(luckyWord.length()));
            return luckyChar;
        }
        else return (char) ('A' + StdRandom.uniform('Z'-'A'+1));
    }

    // fill the given word in the puzzle, if cannot find a space, throw an error.
    private void fillWord(String word) {
        validateWord(word);
        // shuffle the schemes
        Collections.shuffle(schemes);
        Iterator schemeIt = schemes.iterator();
        Pos currentScheme = null;
        ArrayList<Pos> possibleStarts = new ArrayList<>(); // collect all possible spots with this scheme, and later pick a random one.
        do {
            // if last round didn't find even one spot, get the next scheme.
            currentScheme = schemeIt.hasNext() ? ((Pos) schemeIt.next()).getCopy() : null; // get a copy of the scheme selected.
            if (currentScheme == null) throw new RuntimeException("Cannot find a spot using all schemes in the puzzle while filling the word " + word);

            // for every position in the map, if there is space, push it into the queue.
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    currentScheme.reset(r, c);
                    if (hasSpaceForWord(currentScheme, word)) possibleStarts.add(currentScheme.getCopy());
                }
            }
            if (possibleStarts.isEmpty()) {
                System.out.println("No space found with " + currentScheme.getClass().toString() + " on word " + word + ". Trying to switch to the next scheme");
                continue;
            }
            else break;
        } while (true);
        // get a random one and fill it.
        Pos currentPos = possibleStarts.get(StdRandom.uniform(possibleStarts.size()));
        int currentIndex = 0;
        while (currentIndex < word.length()) // for every index in the word
        {
            // if there is no for the current scheme.
            if (currentPos == null) throw new RuntimeException(String.format("No space when filling word %s, check your 'hasSpaceForWord' method.", word));
            puzzle[currentPos.getRow()][currentPos.getCol()] = word.charAt(currentIndex); // fill char into the puzzle array
            answer[currentPos.getRow()][currentPos.getCol()] = word.charAt(currentIndex); // fill char into the answer array
            currentPos = currentPos.next(); // advance Pos
            currentIndex++; // advance index
        }
    }
    // given a starting Pos, check if there is space to put a certain word.
    private boolean hasSpaceForWord(Pos start, String word) {
        // not checking for null, simply return false if null is passed.
        // if (start == null) throw new IllegalArgumentException("Internal logic error: Can't pass null to hasSpaceForWord method");
        int currentIndex = 0;
        Pos currentPos = start;
        while (currentIndex < word.length()) // for every index in the word
        {
            if (currentPos == null) return false;
            if (currentPos.val() != nullChar
                    && currentPos.val() != word.charAt(currentIndex))
                return false;
            currentPos = currentPos.next(); // advance Pos
            currentIndex++; // advance index
        }
        return true;
    }
    private void validateWord(String word) {
        if(word.length() > width && word.length() > height) throw new IllegalArgumentException("Word length greater than puzzle size!");
    }



    // An abstract class to model different ways that a word can be laid out in a puzzle (e.g. horizontal, vertical, diagonal, circular...)
    abstract public class Pos {
        protected int row;
        protected int col;
        public Pos(int r, int c){
            reset(r,c);
        }
        public void reset(int r, int c){
            if (!isInbound(r,c)) throw new IllegalArgumentException(String.format("Illegal row or col (%d, %d) given to construct a Pos object",r,c));
            row = r;
            col = c;
        }
        public int getRow() { return row; }
        public int getCol() { return col; }
        public char val() { return puzzle[row][col]; }
        public String getString() // get the string from beginning to end in the current scheme
        {
            Pos currentChar = this.getCopy(); // get a copy of current pos
            while (currentChar.prev() != null) currentChar = currentChar.prev(); // go to beginning
            StringBuilder sb = new StringBuilder();
            while (currentChar != null) {
                sb.append(currentChar.val());
                currentChar = currentChar.next();
            }
            return sb.toString();
        }
        public int getIndexInString() // get the index of current position returned by getString()
        {
            Pos currentChar = this.getCopy(); // get a copy of current pos
            int index = 0;
            currentChar = currentChar.prev();
            while (currentChar != null) {
                currentChar = currentChar.prev(); // go to beginning
                index++;
            }
            return index;
        }
        abstract public Pos getCopy(); // get a copy of current Pos, serves as a constructor to avoid using reflect in Java
        abstract public Pos prev(); // get the previous according to a scheme (e.g. horizontal), return null if it is the beginning.
        abstract public Pos next(); // get the next according to a scheme, return null if it is the end.
    }

    // Horizontal scheme of word layout
    private class HorizontalPos extends Pos {
        public HorizontalPos(int r, int c) {
            super(r, c);
        }
        @Override
        public Pos getCopy() {
            return new HorizontalPos(row, col);
        }
        @Override
        public Pos prev() {
            if (col == 0) return null;
            return new HorizontalPos(row, col-1);
        }
        @Override
        public Pos next() {
            if (col == width-1) return null;
            return new HorizontalPos(row, col+1);
        }
    }
    // Vertical scheme of word layout
    private class VerticalPos extends Pos {
        public VerticalPos(int r, int c) {
            super(r, c);
        }
        @Override
        public Pos getCopy() {
            return new VerticalPos(row, col);
        }
        @Override
        public Pos prev() {
            if (row == 0) return null;
            return new VerticalPos(row-1, col);
        }
        @Override
        public Pos next() {
            if (row == height-1) return null;
            return new VerticalPos(row+1, col);
        }
    }
    // LeftBottomRightTop scheme of word layout
    private class LeftBottomRightTopPos extends Pos {
        public LeftBottomRightTopPos(int r, int c) {
            super(r, c);
        }
        @Override
        public Pos getCopy() {
            return new LeftBottomRightTopPos(row, col);
        }
        @Override
        public Pos prev() {
            if (col == 0 || row == height-1) return null;
            return new LeftBottomRightTopPos(row+1, col-1);
        }
        @Override
        public Pos next() {
            if (row == 0 || col == height-1) return null;
            return new LeftBottomRightTopPos(row-1, col+1);
        }
    }
    // LeftTopRightBottom scheme of word layout
    private class LeftTopRightBottom extends Pos {
        public LeftTopRightBottom(int r, int c) {
            super(r, c);
        }
        @Override
        public Pos getCopy() {
            return new LeftTopRightBottom(row, col);
        }
        @Override
        public Pos prev() {
            if (col == 0 || row == 0) return null;
            return new LeftTopRightBottom(row-1, col-1);
        }
        @Override
        public Pos next() {
            if (col == height-1 || row == height-1) return null;
            return new LeftTopRightBottom(row+1, col+1);
        }
    }
    // define more schemes here! e.g. reversedHorizontal, spinningOutward...
    // and add it to the constructor!

    private boolean isInbound(int r, int c) {
        if (r >= 0 && r < height && c >= 0 && c < width) return true;
        else return false;
    }
    public char[][] getPuzzle() {
        return puzzle;
    }
    public void printPuzzle() {
        for (int row=0; row<height; row++) {
            for (int col=0; col<width; col++)
                System.out.format("%c ", puzzle[row][col]);
            System.out.println();
        }
    }
    public char[][] getAnswer() {
        return puzzle;
    }
    public void printAnswer() {
        for (int row=0; row<height; row++) {
            for (int col=0; col<width; col++)
                System.out.format("%c ", answer[row][col]);
            System.out.println();
        }
    }


    public static void main (String[] args) throws Exception {
        PuzzleGenerator pg = new PuzzleGenerator(100,100,new String[]{
                "Merry",
                "Christmas",
                "Christ"
        });
        pg.printPuzzle();
        StdOut.println();
        pg.printAnswer();
    }
}