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
