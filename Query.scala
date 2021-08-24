package search.sol

import main.search.src.FileIO
import java.io._
import scala.collection.mutable.HashMap
import scala.util.matching.Regex
import main.java.search.src.PorterStemmer.stemArray


/**
 * Represents a query REPL built off of a specified index
 *
 * @param titleIndex    - the filename of the title index
 * @param documentIndex - the filename of the document index
 * @param wordIndex     - the filename of the word index
 * @param usePageRank   - true if page rank is to be incorporated into scoring
 */
class Query(titleIndex: String, documentIndex: String, wordIndex: String,
            usePageRank: Boolean) {

  // Maps the document ids to the title for each document
  private val idsToTitle = new HashMap[Int, String]

  // Maps the document ids to the euclidean normalization for each document
  private val idsToMaxFreqs = new HashMap[Int, Double]

  // Maps the document ids to the page rank for each document
  private val idsToPageRank = new HashMap[Int, Double]

  // Maps each word to a map of document IDs and frequencies of documents that
  // contain that word
  private val wordsToDocumentFrequencies = new HashMap[String, HashMap[Int, Double]]

  /**
   * Handles a single query and prints out results
   *
   * @param userQuery - the query text
   */
  private def query(userQuery: String) {
    val scores: HashMap[Int, Double] = new HashMap[Int, Double]

    val regex = new Regex("""\[\[[^\[]+?\]\]|[^\W_]+'[^\W_]+|[^\W_]+""")
    val queryWords = stemArray(regex.findAllMatchIn(userQuery).toList.map{ aMatch => aMatch.matched}
      .toArray).filter(s => wordsToDocumentFrequencies.contains(s))

    for(word <- queryWords){ // go through all the query words
      for((id, title) <- idsToTitle){ // check each page
        val docFreqs = wordsToDocumentFrequencies(word) // hashmap of doc frequencies for that word
        if(docFreqs.contains(id)){ // check if the word frequency of this word in this page > 0
          if(scores.contains(id)){ // if the document is already in the results
            val prevScore = scores(id)
            scores(id) = prevScore + getScore(docFreqs(id), idsToMaxFreqs(id), word)
            // add the next word's score
          }
          else{ // if the document is not in the results yet
            scores(id) = getScore(docFreqs(id), idsToMaxFreqs(id), word)
            // set the page's score to this word's score
          }
        }
      }
    }
    if(scores.size == 0 ) { // if there are not docs with any of the words
      println("no results")
      return // exit the query
    }
    if(usePageRank){ // if page rank is requested
      for((pageID, pageScore) <- scores){ // go through all the pages and their scores
        scores(pageID) = pageScore * idsToPageRank(pageID) // recalculate their score with page rank, and reset
      }
    }
    var size = 10
    if(scores.size<10){ size = scores.size }
    printResults(scores.toSeq.sorted.reverse.take(size).map(_._1).toArray)
  }

  /**
   * calculates score of a term (tf*idf) in a page
   *
   * @param termFreq Double - number of times the term appears in the page
   * @param maxFreq Double - number of times the most popular term appears in the page
   * @param word String - the given term
   * @return
   */
  def getScore(termFreq : Double, maxFreq : Double, word : String): Double ={
    val tf = termFreq/maxFreq

    val numPages : Double = idsToTitle.size
    var numContainingWord = 0.0
    for((id, freq) <- wordsToDocumentFrequencies(word)){
      if(freq > 0 && (idsToTitle(id) != word)) {
        numContainingWord = numContainingWord + 1.0
      }
    }
    val idf = Math.log(numPages/numContainingWord)
    idf*tf
  }

  /**
   * Format and print up to 10 results from the results list
   *
   * @param results - an array of all results to be printed
   */
  private def printResults(results: Array[Int]) {
    for (i <- 0 until Math.min(10, results.size)) {
      println("\t" + (i + 1) + " " + idsToTitle(results(i)))
    }
  }

  /**
   * Reads in the text files.
   */
  def readFiles(): Unit = {
    FileIO.readTitles(titleIndex, idsToTitle)
    FileIO.readDocuments(documentIndex, idsToMaxFreqs, idsToPageRank)
    FileIO.readWords(wordIndex, wordsToDocumentFrequencies)
  }

  /**
   * Starts the read and print loop for queries
   */
  def run() {
    val inputReader = new BufferedReader(new InputStreamReader(System.in))

    // Print the first query prompt and read the first line of input
    print("search> ")
    var userQuery = inputReader.readLine()

    // Loop until there are no more input lines (EOF is reached)
    while (userQuery != null) {
      // If ":quit" is reached, exit the loop
      if (userQuery == ":quit") {
        inputReader.close()
        return
      }

      // Handle the query for the single line of input
      query(userQuery)

      // Print next query prompt and read next line of input
      print("search> ")
      userQuery = inputReader.readLine()
    }

    inputReader.close()
  }
}

object Query {
  def main(args: Array[String]) {
    try {
      // Run queries with page rank
      var pageRank = false
      var titleIndex = 0
      var docIndex = 1
      var wordIndex = 2
      if (args.size == 4 && args(0) == "--pagerank") {
        pageRank = true;
        titleIndex = 1
        docIndex = 2
        wordIndex = 3
      } else if (args.size != 3) {
        println("Incorrect arguments. Please use [--pagerank] <titleIndex> "
          + "<documentIndex> <wordIndex>")
        System.exit(1)
      }
//      val query: Query = new Query(args(titleIndex), args(docIndex), args(wordIndex), pageRank)
      val query: Query = new Query(
        "/Users/kleoku/Desktop/brown/spring2021/CS0180/IntelliJ/search-kku2-avonderg/titles.txt",
        "/Users/kleoku/Desktop/brown/spring2021/CS0180/IntelliJ/search-kku2-avonderg/docs.txt",
        "/Users/kleoku/Desktop/brown/spring2021/CS0180/IntelliJ/search-kku2-avonderg/words.txt",
        false)

      query.readFiles()
      query.run()
    } catch {
      case _: FileNotFoundException =>
        println("One (or more) of the files were not found")
      case _: IOException => println("Error: IO Exception")
    }
  }
}
