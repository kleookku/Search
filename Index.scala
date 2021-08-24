package search.sol

import main.java.search.src.StopWords.{isStopWord, words}

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.util.matching.Regex
import scala.xml.Node
import main.java.search.src.PorterStemmer.stemArray
import main.search.src.FileIO.{printDocumentFile, printTitleFile, printWordsFile}

/**
 * Provides an XML indexer, produces files for a querier
 *
 * @param inputFile - the filename of the XML wiki to be indexed
 */
class Index(val inputFile: String) {
  // Maps the document ids to the title for each document
  // we made these fields not private so they could be used in the tester
  private val idsToTitle = new scala.collection.mutable.HashMap[Int, String]

  // Maps the document ids to the euclidean normalization for each document
  private val idsToMaxFreqs = new scala.collection.mutable.HashMap[Int, Double]

  // Maps the document ids to the page rank for each document
  private var idsToPageRank = new scala.collection.mutable.HashMap[Int, Double]

  // Maps each word to a map of document IDs and frequencies of documents that
  // contain that word
  private var wordsToDocumentFrequencies =
  new scala.collection.mutable.HashMap[String, scala.collection.mutable.HashMap[Int, Double]]

  // Maps each word to a Array of the titles of the links on that page
  private val idsToLinks = new scala.collection.mutable.HashMap[Int, ArrayBuffer[String]]

  // Maps the document ids to the title for each document
  private val idsToText = new scala.collection.mutable.HashMap[Int, ArrayBuffer[String]]

  def getIdsToTitle(id : Integer)={ idsToTitle(id) }
  def getIdsToMaxFreqs(id : Integer)={ idsToMaxFreqs(id) }
  def getIdsToPageRank(id : Integer)={ idsToPageRank(id) }

  /**
   * creates the indexer files (titles.txt, docs.txt, words.txt)
   */
  def createIndex = {
    val fileNode: Node = xml.XML.loadFile(inputFile)
    val nodes: Array[Node] = (fileNode \\ "page").toArray // array of nodes, each node is a page
    val numPages = nodes.size
    val regex = new Regex("""\[\[[^\[]+?\]\]|[^\W_]+'[^\W_]+|[^\W_]+""")

    for (node <- nodes) { // iterates through each page
      val id = (node \\ "id").text.replace("\n", "")
        .replace(" ", "").toInt
      val title = (node \\ "title").text.replace("\n", "").toLowerCase()
      val text = (node \\ "text").text.replace("\n", "").toLowerCase() + " " + title

      idsToTitle(id) = title
      val textArray = stemArray(regex.findAllMatchIn(text).toList.map{aMatch => aMatch.matched}
        .filter(s => !isStopWord(s)).toArray.map(_.toLowerCase))

      idsToText(id) = ArrayBuffer(textArray: _*)

      //for finding popular term
      var uniqueWords: ArrayBuffer[String] = ArrayBuffer.empty
      var wordCounts: ArrayBuffer[Double] = ArrayBuffer.empty
      val pageTitle = idsToTitle(id)

      for (i <- 0 to idsToText(id).size-1) { // iterate through each word on the page
        val word = idsToText(id)(i)

        // setting link list
        if (isLink(word)) { // if the word is a link
          val linkTitle : String = linkToTitle(word)
          if(linkTitle == pageTitle){ // if the link is to itself
            if(!idsToLinks.contains(id)){ idsToLinks(id) = ArrayBuffer.empty }
          }
          else if (idsToLinks.contains(id)) { // if the id is already stored
            if(!idsToLinks(id).contains(linkTitle)){ // if it doesn't already contain the link
              val newLinks = idsToLinks(id) :+ linkTitle
              idsToLinks(id) = newLinks // add the link to the link array
            }
          } else if (!idsToLinks.contains(id)){ // if the id is not stored yet
            // make a new Array buffer and store the link
            idsToLinks(id) = ArrayBuffer(linkTitle)
          }
          idsToText(id) = idsToText(id) ++ linkToWord(word)// add the tokenized link words to the words array
          idsToText(id)(i) = "" // replace the link with an empty string
        }
        // if the page has no links, set its links array to an empty array
        if(!idsToLinks.contains(id)){ idsToLinks(id) = ArrayBuffer.empty}

        //finding popular term
        if (uniqueWords.contains(word)) {
          val index: Int = uniqueWords.indexOf(word)
          val count: Double = wordCounts(index)
          wordCounts(index) = count + 1
        } else {
          val newCount: Double = 1
          val newUniqueWords = uniqueWords :+ word
          uniqueWords = newUniqueWords
          val newWordCounts = wordCounts :+ newCount
          wordCounts = newWordCounts
        }

        // adding unique words to wordsToDocumentFrequencies
        if (!wordsToDocumentFrequencies.contains(word) && !isLink(word)) {
          wordsToDocumentFrequencies(word) = new scala.collection.mutable.HashMap[Int, Double]
          wordsToDocumentFrequencies(word)(id) = findTermCount(idsToText(id), word)
        } else if(wordsToDocumentFrequencies.contains(idsToText(id)(i)) && !isLink(idsToText(id)(i))){
          if(!wordsToDocumentFrequencies(word).contains(id)){
            wordsToDocumentFrequencies(word)(id) = findTermCount(idsToText(id), word)
          }
        }
      }

      // set max word count
      idsToMaxFreqs(id) = wordCounts.max
    }
    idsToPageRank = pageRank(numPages)
    printFiles
  }

  /**
   * writes the hashmaps into the files (call methods from FileIO)
   * titles.txt - idToTitle
   * docs.txt - idsToMaxFreqs, idsToPageRank
   * words.txt - wordsToDocumentFrequencies
   */
  def printFiles = {
    printTitleFile("titles.txt", idsToTitle)
    printDocumentFile("docs.txt", idsToMaxFreqs, idsToPageRank)
    printWordsFile("words.txt", wordsToDocumentFrequencies)
  }

  /**
   * returns true if input word is in the format of a link
   * otherwise returns false
   * @param word String - input word
   * @return Boolean - whether or not input word is a link
   */
  def isLink(word: String): Boolean = {
    val linkRegex = new Regex("""\[\[.*]]""") // anything in the format [[...]]] is a link
    if (linkRegex.matches(word)) { return true }
    false
  }

  /**
   * extracts the text part of a link, depending on the format of the link
   * @param link String - the input link
   * @return Array[String] - an Array of the relevant words in the link
   */
  def linkToWord(link: String): Array[String] = {
    if (link.contains("|")) {
      var linkWord = link.substring(link.indexOf("|") + 1) // remove the title part
      linkWord.split(" |") // split the words into an array
    } else { // if there's no bar, just split the words on colons and spaces
      link.substring(2, link.size - 2).split("""[:\s]""")
    }
  }

  /**
   * extracts the title of the page that the link directs to
   * @param link String - the input link
   * @return String - the title of the page
   */
  def linkToTitle(link: String): String = {
    val noBrackets = link.replace("[", "").replace("]", "")
        if(noBrackets.contains("|")){
          return noBrackets.substring(0, noBrackets.indexOf("|")).toLowerCase()
        }
    noBrackets.toLowerCase()
  }

  /**
   * finds the number of times the input term appears in the page's text (array of words)
   * called by findDocFrequencies()
   * @param words Array[String] - array of words in the page
   * @param term String - the term whose count the method finds
   * @return
   */
  def findTermCount(words: ArrayBuffer[String], term: String): Double = {
    var count: Double = 0
    for (word <- words) {
      if (word == term) {
        count = count + 1
      }
    }
    count
  }

  /**
   *
   * fills out idsToPageRank hashmap
   * calculates pageRank for each page and puts it in the hashmap
   *
   * @param numPages - number of pages in the corpus
   * @return idsToPageRank HashMap[Int, Double]
   */
  def pageRank(numPages: Double): scala.collection.mutable.HashMap[Int, Double] = {
    // ID -> PageRank (previous iteration)
    var previous = idsToTitle.clone() map{ case( (id, title)) => (id, 0.0)}
    // ID -> PageRank (current iteration)
    val current = idsToTitle.clone() map{ case( (id, title)) => (id, 1/numPages)}
    // weights for each page link pair
    val weights = getWeights(numPages)

    while (distance(previous, current) > 0.001) { // until the page ranks are stabilized
      previous = previous.map {case (k, v) => (k, current(k))}
        for ((curID, curRank) <- current) {
          current.update(curID, 0.0)// set each pageRank in the hashmap to 0.0
          for ((prevID, prevRank) <- previous) {
            val weight = weights(curID)(prevID)
            val add = weight * prevRank
            current(curID) = current(curID) + add
          }
      }
    }
    current
  }

  /**
   * for-loop
   * calcualtes the weights of each page in the corpus and return a hashmap
   * that maps page ids to the page's weight
   * @param numPages - number of pages in the corpus
   * @return HashMap[Int, Double] hashmap of page ids to their weights
   */
  def getWeights(numPages: Double):
  scala.collection.mutable.HashMap[Int, scala.collection.mutable.HashMap[Int, Double]] = {
    val weights = scala.collection.mutable.HashMap[Int, scala.collection.mutable.HashMap[Int, Double]]()
    val epsilon = .15

    for ((id, allLinks) <- idsToLinks) {
      val links: ArrayBuffer[String] = inCaseEmpty(allLinks, id)
      val numLinks: Double = links.size
        for ((linkedPageID, linkedPageTitle) <- idsToTitle) {
          if (!weights.contains(linkedPageID)) {
            weights(linkedPageID) = new scala.collection.mutable.HashMap[Int, Double]()
          }
          if (links.contains(linkedPageTitle)) { // if k links to j
            weights(linkedPageID)(id) = (epsilon / numPages) + ((1 - epsilon) * (1 / numLinks))
          } else if (!links.contains(linkedPageTitle)) { // if k doesn't link to j
            weights(linkedPageID)(id) = epsilon / numPages
          }
        }
      }
    weights
    }

  /**
   * if the page has no links, set its link array to all the titles except its own
   * @param links Array[String] - array of links on that page
   * @param id Integer - id of the page
   * @return Array[String] - array of links with special cases handled
   */
  def inCaseEmpty(links: ArrayBuffer[String], id: Integer): ArrayBuffer[String] = {
    // remove links to outside the corpus and duplicate links
    var newLinkArray: ArrayBuffer[String] = links.clone().filter(s => idsToTitle.values.exists(_==s)).distinct
    if(newLinkArray.isEmpty){ // if there are no links on the page
      // set the link array to all the titles except itself (it links to every page except itself)
      newLinkArray = ArrayBuffer(idsToTitle.values.toArray:_*).dropWhile(s=>s==idsToTitle(id))
    }
    newLinkArray
  }

  /**
   * calculates the distance between two hashmaps of pageRanks
   * @param previous HashMap[Int, Double] previous iteration of pageRanks
   * @param current HashMap[Int, Double] current iteration of pageRanks
   * @return
   */
  def distance(previous: HashMap[Int, Double], current: HashMap[Int, Double]): Double = {
    // distance = square root of (the sum of all the differences^2)
    var differenceSum = 0.0
    for ((id, prevPR) <- previous) {
        differenceSum += Math.pow(prevPR - current(id), 2)
    }
    Math.sqrt(differenceSum)
  }
}
object Index {
  def main(args: Array[String]) {
    val inputFile = "/Users/kleoku/Desktop/brown/spring2021/CS0180/IntelliJ" +
      "/search-kku2-avonderg/src/main/search/src/MedWiki.xml"
    val indexer = new Index(inputFile)
    indexer.createIndex
  }
}
