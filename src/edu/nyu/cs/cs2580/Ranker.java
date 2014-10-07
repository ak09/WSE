package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Scanner;

class Ranker {
	private Index _index;
	private Double cosineBeta = new Double(1);
	private Double QLBeta = new Double(1);
	private Double phraserBeta = new Double(1);
	private Double numViewsBeta = new Double(1);
	private final Double lambda = new Double(0.5);
	private boolean unigramQuery = Boolean.FALSE;
	
	public Ranker(String index_source){
		_index = new Index(index_source);
	}
	
	private enum RankerTypes{
		COSINE,QL,PHRASER,NUM_VIEWS;
	}
	
	public Vector < ScoredDocument > runquery(String query){
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(runquery(query, i));
		}
		return retrieval_results;
	}

	public ScoredDocument runquery(String query, int did){

		// Build query vector
		Scanner s = new Scanner(query);
		Vector < String > qv = new Vector < String > ();
		while (s.hasNext()){
			String term = s.next();
			qv.add(term);
		}
		s.close();
		// Get the document vector. For hw1, you don't have to worry about the
		// details of how index works.
		Document d = _index.getDoc(did);
		Vector < String > dv = d.get_title_vector();

		// Score the document. Here we have provided a very simple ranking model,
		// where a document is scored 1.0 if it gets hit by at least one query term.
		double score = 0.0;

		for (int i = 0; i < dv.size(); ++i){
			for (int j = 0; j < qv.size(); ++j){
				if (dv.get(i).equals(qv.get(j))){
					score = 1.0;
					break;
				}
			}
		}
		return new ScoredDocument(did, d.get_title_string(), score);
	}

	public Vector < ScoredDocument > QLRunQuery(String query){
		Map<String,Double> queryProbMap = makeQLQueryProbMap(query);
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		
		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(QLRunQuery(queryProbMap, i));
		}
		
		sortAccordingToScore(retrieval_results);
		return retrieval_results;
	}
	
	private Map<String,Double> makeQLQueryProbMap(String query){
		Map<String,Double> queryProbMap = new HashMap<String,Double>();
		int vocabSize = _index.termFrequency();
		for (String term : query.split(" ")){
			updateMap(queryProbMap,term);
		}
		for(String qTerm: queryProbMap.keySet()){
			Double termFreq = new Double( _index.termFrequency(qTerm));
			queryProbMap.put(qTerm, (lambda*(termFreq/vocabSize)));
		}
		return queryProbMap;
	}

	private ScoredDocument QLRunQuery(Map<String,Double> queryProbMap,int did){
		Document d = _index.getDoc(did);
		Map<String,Double> docStrings = new HashMap<String,Double>();
		Double QL = new Double(0);
		int docSize = d.get_title_vector().size() + d.get_body_vector().size();
		for(String term: d.get_body_vector()){
			updateMap(docStrings,term);
		}
		//System.out.println("doc -> "+ d.get_title_string());
		for(String s : queryProbMap.keySet()){
			Double temp = new Double(0);
			if(docStrings.containsKey(s)){
				temp = docStrings.get(s);
				temp = (1 - lambda)*(temp/docSize);
				temp += queryProbMap.get(s);
			}
			else{
				temp = queryProbMap.get(s);
			}
			QL += (Math.log(temp)/Math.log(2));
		}
		return new ScoredDocument(did, d.get_title_string(), QL);
	}

	private void updateMap(Map<String,Double> docMap, String term){
		if(docMap.containsKey(term)){
			docMap.put(term, (docMap.get(term) + 1)); 
		}
		else{
			docMap.put(term, new Double(1));
		}
	}

	private void sortAccordingToScore(Vector <ScoredDocument> results){
		Collections.sort(results,new Comparator<ScoredDocument>(){
			@Override
			public int compare(ScoredDocument sd1, ScoredDocument sd2) {
				if(sd1._score < sd2._score){
					return 1;
				}
				else if(sd1._score > sd2._score){
					return -1;
				}
				return 0;
			}
		});
	}

	public Vector < ScoredDocument > runquery_numviews(String query){
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		for (int i = 0; i < _index.numDocs(); ++i){
			ScoredDocument sd = runquery_numviews(i);
			retrieval_results.add(sd);
		}
		sortAccordingToScore(retrieval_results);
		return retrieval_results;
	}

	public ScoredDocument runquery_numviews(int did){
		// Build query vector
		return new ScoredDocument(did, _index.getDoc(did).get_title_string(), _index.getDoc(did).get_numviews());
	}

	/*
	 * Phraser Ranker
	 */
	private void updateMapForBigrams(Map<String,Double> docMap,
			Vector<String> vec){
		for(int i = 1; i<vec.size(); i++){
			String sc = (vec.get(i-1)+" "+vec.get(i));
			if (docMap.containsKey(sc)) {
				docMap.put(sc, (docMap.get(sc) + 1));
			}
			else{
				docMap.put(sc, new Double(1));
			}
		}
	}

	private void updateMapForUnigrams(Map<String,Double> docMap,
			Vector<String> vec){
		for(int i = 0; i<vec.size(); i++){
			if (docMap.containsKey(vec.get(i))) {
				docMap.put(vec.get(i), (docMap.get(vec.get(i)) + 1));
			}
			else{
				docMap.put(vec.get(i), new Double(1));
			}
		}
	}

	public Vector < ScoredDocument > runPhraserQuery(String query){
		Vector < ScoredDocument > retrieval_results =
				new Vector < ScoredDocument > ();

		Map<String, Double> queryVector = getQueryPhraserVec(query);

		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(runPhraserRanker(i, queryVector));
		}
		sortAccordingToScore(retrieval_results);
		return retrieval_results;
	}

	/*
	 * Query broken into uni/bigrams
	 */
	public Map<String, Double> getQueryPhraserVec(String query) {
		// Build query vector
		Scanner sc = new Scanner(query);
		Vector < String > qv = new Vector < String > ();
		while (sc.hasNext()){
			String term = sc.next();
			qv.add(term);
		}
		sc.close();
		Map<String,Double> queryBigrams = new HashMap<String,Double>();

		if (qv.size()>1)
			updateMapForBigrams(queryBigrams, qv);
		else
			updateMapForUnigrams(queryBigrams, qv);

		return queryBigrams;
	}

	/*
	 * doc broken into uni/bigrams
	 */
	public ScoredDocument runPhraserRanker(int did, Map<String,Double> queryBig){
		Document d = _index.getDoc(did);

		Map<String,Double> docBigrams = new HashMap<String,Double>();

		if(queryBig.size()>1){
			//updateMapForBigrams(docBigrams, d.get_title_vector());
			updateMapForBigrams(docBigrams, d.get_body_vector());
		}
		else {
			//updateMapForUnigrams(docBigrams, d.get_title_vector());
			updateMapForUnigrams(docBigrams, d.get_body_vector());
		}
		
		double score = 0.0;
		for(Map.Entry<String, Double> queryBigram : queryBig.entrySet()){
			if(docBigrams.containsKey(queryBigram.getKey())){
				double occurences = docBigrams.get(queryBigram.getKey());
				score = score + occurences;
			}
		}
		return new ScoredDocument(did, d.get_title_string(), score);
	}

	/*
	 * Cosine Ranker
	 */
	public Vector < ScoredDocument > runCosineQuery(String query){
		Vector < ScoredDocument > retrieval_results =
				new Vector < ScoredDocument > ();

		Map<String, Double> queryVector = getQueryCosineVec(query);

		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(runCosineRanker(i, queryVector));
		}
		sortAccordingToScore(retrieval_results);
		return retrieval_results;
	}

	public Map<String, Double> getQueryCosineVec(String query) {

		double idfNumerator = _index.numDocs();

		// Build query vector
		Scanner sc = new Scanner(query);
		Vector < String > qv = new Vector < String > ();
		while (sc.hasNext()){
			String term = sc.next();
			qv.add(term);
		}
		sc.close();

		Map<String,Double> tfNumerator = new HashMap<String,Double>();
		for(String s: qv){
			updateMap(tfNumerator, s);
		}

		double tfDenominator = qv.size();

		Map <String, Double> queryVector =
				new HashMap<String, Double>();
		for(String s: tfNumerator.keySet()){
			double tf = tfNumerator.get(s)/tfDenominator;
			double idf = 1+ ((Math.log(idfNumerator/ _index.documentFrequency(s)))/Math.log(2));
			queryVector.put(s, (tf*idf));
		}
		return queryVector;
	}

	public ScoredDocument runCosineRanker(int did, Map<String,Double> queryVec){

		double idfNumerator = _index.numDocs();

		Document d = _index.getDoc(did);

		Map<String,Double> tfNumerator = new HashMap<String,Double>();
		/*for(String s: d.get_title_vector()){
			updateMap(tfNumerator, s);
		}
		*/
		for(String s: d.get_body_vector()){
			updateMap(tfNumerator, s);
		}

		double tfDenominator = (d.get_title_vector().size()) +
				(d.get_body_vector().size());
		Map <String, Double> docVec = new HashMap<String, Double>();
		for(String s: tfNumerator.keySet()){
			double tf = tfNumerator.get(s)/tfDenominator;
			double idf =1+ ((Math.log(idfNumerator/ _index.documentFrequency(s)))/Math.log(2));
			docVec.put(s, (tf*idf));
		}

		double scoreNumerator = 0.0;
		for(Map.Entry<String, Double> queryWord : queryVec.entrySet()){
			if(docVec.containsKey(queryWord.getKey())){
				double firstTerm = docVec.get(queryWord.getKey());
				double secondTerm = queryVec.get(queryWord.getKey());
				scoreNumerator = scoreNumerator + (firstTerm*secondTerm);
			}
		}

		double scoreDenominatorFirstTerm = 0.0;
		for(Map.Entry<String, Double> queryWord : queryVec.entrySet()){
			double queryWrdSq = (queryWord.getValue() * queryWord.getValue());
			scoreDenominatorFirstTerm = scoreDenominatorFirstTerm + (queryWrdSq);
		}

		double scoreDenominatorSecondTerm = 0.0;
		for(Map.Entry<String, Double> docWord : docVec.entrySet()){
			double docWrdSq = (docWord.getValue() * docWord.getValue());
			scoreDenominatorSecondTerm = scoreDenominatorSecondTerm + (docWrdSq);
		}

		double scoreDenominator = Math.sqrt((scoreDenominatorFirstTerm *
				scoreDenominatorSecondTerm));

		double score = 0.0;
		if (scoreNumerator > 0 && scoreDenominator > 0){
			score = scoreNumerator/scoreDenominator;
		}
		return new ScoredDocument(did, d.get_title_string(), score);
	}
	
	public Vector < ScoredDocument > linearRunQuery(String query){
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		Map<String,Double> queryProbMap = makeQLQueryProbMap(query);
		if(queryProbMap.size() == 1){
			unigramQuery = Boolean.TRUE;
		}
		for(int i=0;i<_index.numDocs();i++){
			numViewsBeta += _index.getDoc(i).get_numviews();
		}
		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(linearRunQuery(i,query,queryProbMap));
		}
		sortAccordingToScore(retrieval_results);
		return retrieval_results;
	}

	private ScoredDocument linearRunQuery(int did, String query,Map<String,Double> queryProbMap){
		Document d = _index.getDoc(did);
		Double linearScore = new Double(0);
		for(RankerTypes rt: RankerTypes.values()){
			linearScore += linearRunByRankerType(rt,query,did,queryProbMap);
		}
		return new ScoredDocument(did, d.get_title_string(), linearScore);
	}
	
	private Double linearRunByRankerType(RankerTypes rt,String query, int did,Map<String,Double> queryProbMap){
		switch (rt) {
		case QL:
			return (getBeta(rt))*(QLRunQuery(queryProbMap, did)._score);
		case COSINE:
			queryProbMap = getQueryCosineVec(query);
			return (getBeta(rt))*(runCosineRanker(did, queryProbMap)._score);
		case PHRASER:
			queryProbMap = getQueryPhraserVec(query);
			return getBeta(rt)*(runPhraserRanker(did, queryProbMap)._score);
		case NUM_VIEWS:
			return getBeta(rt)*(runquery_numviews(did)._score);
		default:
			return null;
		}
		
	}
	/*
	 * Beta values - QL = 1, Cosine = 1, 
	 * For Phraser if query is unigram then it will be 0 or else it will be 1
	 * For Num_Views - It will be 1/(total num of views in the corpus) 
	*/
	private Double getBeta(RankerTypes rt){
		switch (rt) {
		case QL:
			return QLBeta;
		case COSINE:
			return cosineBeta;
		case PHRASER:
			if(unigramQuery ){
				phraserBeta = 0.0;
			}
			else{
				phraserBeta = 1.0;
			}
			return phraserBeta;
		case NUM_VIEWS:
			return 1.0/numViewsBeta;
		default:
			return 0.0;
		}
	}

}
