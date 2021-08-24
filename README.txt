INSTRUCTIONS FOR USE:
1. In the main method of Index, set the inputFile value to the desired wiki that you want to search through.
2. Run the main method.
3. Set the input configurations for Query to the file names in this order:
       titles doc name, document doc name, word doc name
4. To request to use pagerank to score documents, include --pagerank at the beginning of the configurations.
5. Run the query.
6. When the search> indicator shows up, input your query term.
7. View the results.



DESIGN OVERVIEW:
Index:
The index produces the files of words to documents frequencies, ids to titles,
and ids to  max frequencies and page ranks. First, the indexer loads in the file, processes
the text of each page, and creates the hashmaps. Then, the indexer uses the methods in
FileIO to write these hashmaps into files.

File Descriptions:
-----------Words to document frequences:
- maps each unique word found in all pages to a hashmap
- the hashmap maps doc ids to the number of time the word appears in that page
- if a page doesn't contain that word, it will not be in the hashmap
- if a page's id is not in the hashmap, we know the word frequency is 0 for that page
-----------Ids to titles:
- maps each page's id to the page's title
-----------Ids to Max Frequencies:
- maps each page id to the count of the most popular word on that page
-----------Ids to page rank
- maps each page id to its page rank

Query:
The query reads the files and converts them into hash maps. When a word is sent into
the query, the query calculates the score of the word using the term frequencies and max
frequencies of each document. Finally, the query takes ten documents with the highest scores
and prints them out. If there's fewer than ten documents that contain at least one of
the query words, it prints out those number of documents.




FEATURES:
Failed features:
- n/a
Extra features:
- n/a




BUGS:
- index is slower than desired (MedWiki takes 4 instead of 2 minutes)
    - due to use of linear-time method calls (e.g. exists, map) and for-loops




TESTING:
Wiki: PageRankWiki
Resulting Page Ranks:
- 100 -> 0.851500000000001
- 1 to 99 -> 0.0015000000000000029
Query Results: (input term is "100")
search> 100
	1 100
	2 99
	3 98
	4 97
	5 96
	6 95
	7 94
	8 93
	9 92
	10 91


Wiki: TestWiki1.xml
Input Query Term: word1
Expected Search Results: title1

Wiki: TestWiki2.xml
Input Query Term: word
Expected Search Results: title5, title4, title3, title2, title1

Wiki: TestWiki3.xml
Input Query Term: word
Expected Search Results: title3, title2, title1
Note: In the wiki, title3 links to title2 and title1, and both title1 and title2 link to nothing.
This causes title1 and title2 to fall into a special case, where if a page links to nothing,
we can pretend that it links to all the pages except itself. This causes each page to link
to each other and thus have all the same scores/page ranks.

Wiki: TestWiki4.xml (same as the example given in the handout)
- We tested this in the tester file. In the wiki, and as it is in the handout, A links to B and C,
C links to A, and B links to nothing. The corresponding page ranks the program calculated
(after rounding to the thousandth decimal place) were as follows:
A->.433
B->.234
C->.333

Errors we found and fixed:
- pages with no links were not stored in the idsToLinks hash map, so the program wouldn't be able to
calculate a weight for that page. To fix this, if a page doesn't have any links, we add it to the
idsToLinks hash map and give it an empty string to put int its link array.
- When combining the text with the title to form the entire set of words of the page,
the last word in the text combined with the title. E.g. if the last word is last, and
the title is title, the last item in the entire set of words would be "lasttitle" instead of
"last title". To remedy this, we placed a string with a single space (" ") between all the
text and the appended title.

Our tester file looked like this:
package search.sol
import tester.Tester

object TestIndex {
  private def test(t: Tester): Unit = {
    // test indexer files

    val index2 = new Index("/Users/kleoku/Desktop/brown/spring2021/CS0180/IntelliJ/search-kku2-avonderg/" +
      "src/main/search/src/TestWiki4.xml")
    index2.createIndex

    // test page rank using example from Handout (A, B, C)
    val testIDsToPageRank2 = scala.collection.mutable.HashMap(0->.433, 1->.234, 2->.333)
    BigDecimal(1.23456789).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

    t.checkExpect(
      BigDecimal(index2.getIdsToPageRank(0)).setScale(3,BigDecimal.RoundingMode.HALF_UP).toDouble,
      testIDsToPageRank2(0))
    t.checkExpect(
      BigDecimal(index2.getIdsToPageRank(1)).setScale(3,BigDecimal.RoundingMode.HALF_UP).toDouble
      , testIDsToPageRank2(1))
    t.checkExpect(
      BigDecimal(index2.getIdsToPageRank(2)).setScale(3,BigDecimal.RoundingMode.HALF_UP).toDouble,
      testIDsToPageRank2(2))

    val index = new Index("/Users/kleoku/Desktop/brown/spring2021/CS0180/IntelliJ/search-kku2-avonderg/" +
      "src/main/search/src/TestWiki1.xml")
    index.createIndex

    val testIDsToTitle = scala.collection.mutable.HashMap(0->"title1", 1->"title2")
    t.checkExpect(index.getIdsToTitle(0), testIDsToTitle(0))
    t.checkExpect(index.getIdsToTitle(1), testIDsToTitle(1))

    val testIDsToMaxFreqs = scala.collection.mutable.HashMap(0->1, 1->1)
    t.checkExpect(index.getIdsToMaxFreqs(0), testIDsToMaxFreqs(0))
    t.checkExpect(index.getIdsToMaxFreqs(1), testIDsToMaxFreqs(1))

    t.checkExpect(index.getIdsToTitle(0), testIDsToTitle(0))
    t.checkExpect(index.getIdsToTitle(1), testIDsToTitle(1))

    // test querier
    val query: Query = new Query(
      "/Users/kleoku/Desktop/brown/spring2021/CS0180/IntelliJ/search-kku2-avonderg/titles.txt",
      "/Users/kleoku/Desktop/brown/spring2021/CS0180/IntelliJ/search-kku2-avonderg/docs.txt",
      "/Users/kleoku/Desktop/brown/spring2021/CS0180/IntelliJ/search-kku2-avonderg/words.txt",
      true)
    query.readFiles()
    val word = "word1"
    val numPages = 2.0
    val numPagesContainingWord = 1.0
    t.checkExpect(query.getScore(1, 1, word), Math.log(numPages/numPagesContainingWord))
  }

  def main(args : Array[String]): Unit = {
    Tester.run(TestIndex)
  }

}


COLLABORATION:
Alex Von Der Goltz (avonderg)
Kleo Ku (kku2)
