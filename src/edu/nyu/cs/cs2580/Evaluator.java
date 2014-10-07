package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Vector;
import java.util.HashMap;
import java.util.Scanner;

class Evaluator {
	private enum TYPES{
		PRECISION_1,PRECISION_5,PRECISION_10,
		RECALL_1,RECALL_5,RECALL_10,
		F_1,F_5,F_10,
		NDCG_1,NDCG_5,NDCG_10,
		PRECISIONATRECALLPOINTS,AVG,RECIPROCALRANK;
		
		public String toString(){
			switch(this){
			case PRECISION_1:
				return "Precision@1";
			case PRECISION_5:
				return "Precision@5";
			case PRECISION_10:
				return "Precision@10";
			case RECALL_1:
				return "Recall@1";
			case RECALL_5:
				return "Recall@5";
			case RECALL_10:
				return "Recall@10";
			case F_1:
				return "F0.50@1";
			case F_5:
				return "F0.50@5";
			case F_10:
				return "F0.50@10";
			case NDCG_1:
				return "NDCG@1";
			case NDCG_5:
				return "NDCG@5";
			case NDCG_10:
				return "NDCG@10";
			case AVG:
				return "Average Precion";
			case PRECISIONATRECALLPOINTS:
				return "PrecisionAtRecallPoints";
			case RECIPROCALRANK:
				return "Reciprocal Rank";
			default:
				break;	
			}
			return null;
		}
	}

	/*
	 * total_relavent_doc is a hashmap containing query and 
	 * total number of "relavent" docs for that query. reqd for Recall.
	 */
	private static HashMap <String, Integer> total_relavent_doc = 
			new HashMap <String, Integer>();
	private static final int PRECISION_1K = 1;
	private static final int PRECISION_5K = 5;
	private static final int PRECISION_10K = 10;
	private static final Double alpha = new Double(.5);
	private static double relevancy = 0;
	private static double Avg_Sum = 0;
	private static HashMap <Double, Double> F_all_points = 
			new HashMap <Double, Double>();
	private static String queryPrint = null;
	
	public static void main(String[] args) throws IOException {
		HashMap < String , HashMap < Integer , Double > > relevance_judgments =
				new HashMap < String , HashMap < Integer , Double > >();
		HashMap < String , HashMap < Integer , Double > > relevance_judgmentsForNDCG =
				new HashMap < String , HashMap < Integer , Double > >();

		if (args.length < 1){
			System.out.println("need to provide relevance_judgments");
			return;
		}
		String p = args[0];
		// first read the relevance judgments into the HashMap
		readRelevanceJudgments(p,relevance_judgments,relevance_judgmentsForNDCG);
		// now evaluate the results from stdin
		evaluateStdin(relevance_judgments,relevance_judgmentsForNDCG);
	}

	public static void readRelevanceJudgments(
			String p,HashMap < String , HashMap < Integer , Double > > relevance_judgments, 
			HashMap < String , HashMap < Integer , Double > > relevance_judgmentsForNDCG){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(p));
			try {
				String line = null;
				while ((line = reader.readLine()) != null){
					// parse the query,did,relevance line
					Scanner s = new Scanner(line).useDelimiter("\t");
					String query = s.next();
					int did = Integer.parseInt(s.next());
					String grade = s.next();
					double rel = 0.0;
					// convert to binary relevance
					updateRelevanceJudgmentsForNDCG(grade,query,did,relevance_judgmentsForNDCG);
					if ((grade.equals("Perfect")) ||
							(grade.equals("Excellent")) ||
							(grade.equals("Good"))){
						rel = 1.0;
						if(!total_relavent_doc.containsKey(query)){
							total_relavent_doc.put(query, 1);
						}
						else{
							total_relavent_doc.put(query, (total_relavent_doc.get(query) + 1));
						}
					}
					if (!relevance_judgments.containsKey(query)){
						HashMap < Integer , Double > qr = new HashMap < Integer , Double >();
						relevance_judgments.put(query,qr);
					}
					HashMap < Integer , Double > qr = relevance_judgments.get(query);
					qr.put(did,rel);
					s.close();
				}
			} finally {
				reader.close();
			}
		} catch (IOException ioe){
			System.err.println("Oops " + ioe.getMessage());
		}
	}

	private static void updateRelevanceJudgmentsForNDCG(String grade, String query, int did ,
			HashMap < String , HashMap < Integer , Double > > relevance_judgmentsForNDCG){
		Double rel = 0.0;
		if (grade.equals("Perfect")){
			rel = 10.0;
		}
		else if (grade.equals("Excellent")){
			rel = 7.0;
		}
		else if	(grade.equals("Good")){
			rel = 5.0;
		}
		else if	(grade.equals("Fair")){
			rel = 1.0;
		}
		if (!relevance_judgmentsForNDCG.containsKey(query)){
			HashMap < Integer , Double > qr = new HashMap < Integer , Double >();
			relevance_judgmentsForNDCG.put(query,qr);
		}
		HashMap < Integer , Double > qr = relevance_judgmentsForNDCG.get(query);
		qr.put(did,rel);
	}

	public static void evaluateStdin(
			HashMap < String , HashMap < Integer , Double > > relevance_judgments, 
			HashMap < String , HashMap < Integer , Double > > relevance_judgmentsForNDCG){
		// only consider one query per call    
		HashMap < TYPES , Double > evaluators = new HashMap<TYPES , Double>();
		ArrayList<Double> topRelevantDocsGain = new ArrayList<Double>();
		ArrayList<Double> sortedtopRelevantDocsGain = null;
		Boolean firstValue = Boolean.FALSE;
		Double reciprocalRank = new Double(0);
		int query_total_relavent_docs = 0;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			for(TYPES evalType: TYPES.values()){
				evaluators.put(evalType, new Double(0));
			}
			String line = null;
			int count = 0;
			while ((line = reader.readLine()) != null){
				Scanner s = new Scanner(line).useDelimiter("\t");
				String query = s.next();
				queryPrint = query;
				int did = Integer.parseInt(s.next());
				String title = s.next();
				double rel = Double.parseDouble(s.next());
				if (relevance_judgments.containsKey(query) == false){
					throw new IOException("query not found");
				}
				HashMap < Integer , Double > qr = relevance_judgments.get(query);
				HashMap < Integer , Double > qrWithGain = relevance_judgmentsForNDCG.get(query);
				
				/*
		         calculates no of relevant documents in the ranker output and saves the 
		         count into hashmap evaluators this will be common for all the rankers
		         It will only handle for the given evaluator in the enum TYPES
		         As you add new rankers we have to change this to accomodate others
				*/
				/*
				 For recall, need the total number of relavent docs for the given query.
				 that is where the following variable comes in play
				*/
				if(total_relavent_doc.containsKey(query)){
						query_total_relavent_docs = total_relavent_doc.get(query);
				}
				else{
					query_total_relavent_docs = 0;
				}
				if(count < 10){
					if(qrWithGain.containsKey(did)){
						topRelevantDocsGain.add(qrWithGain.get(did));
					}
					else{
						topRelevantDocsGain.add(new Double(0));
					}
				}

				if(isRelevant(qr,did)){
					if(!firstValue){
						firstValue = Boolean.TRUE;
						reciprocalRank = 1.0/(count+1);
					}
					if(count < 1){
						updateType1Map(evaluators);
						updateType5Map(evaluators);
						updateType10Map(evaluators);
					}
					else if(count < 5){
						updateType5Map(evaluators);
						updateType10Map(evaluators);
					}
					else if(count < 10){
						updateType10Map(evaluators);
					}
					relevancy++;
					Avg_Sum += (relevancy/(count+1));
					
					F_all_points.put((relevancy/query_total_relavent_docs),
							(relevancy/(count+1)));
				}
				count++;
			}
		} catch (Exception e){
			System.err.println("Error:" + e.getMessage());
		}
		/*
     calculates the precision value by calling the calculatePrecion method
     For your ranker you will have add a case and calculate the evaluator  
		 */
		Double DCG = null;
		Double idealDCG = null;
		StringBuilder evalOutput = new StringBuilder(queryPrint + "\t");
		
		switch(TYPES.PRECISION_1){
			case PRECISION_1:
				evalOutput.append(calculatePrecion(TYPES.PRECISION_1,evaluators,PRECISION_1K) + "\t");
			case PRECISION_5:
				evalOutput.append(calculatePrecion(TYPES.PRECISION_5,evaluators,PRECISION_5K) + "\t");
			case PRECISION_10:
				evalOutput.append(calculatePrecion(TYPES.PRECISION_10,evaluators,PRECISION_10K) + "\t");
			case RECALL_1:
				evalOutput.append(calculateRecall(TYPES.RECALL_1,evaluators,query_total_relavent_docs) + "\t");
			case RECALL_5:
				evalOutput.append(calculateRecall(TYPES.RECALL_5,evaluators,query_total_relavent_docs) + "\t");
			case RECALL_10:
				evalOutput.append(calculateRecall(TYPES.RECALL_5,evaluators,query_total_relavent_docs) + "\t");
			case F_1:
				evalOutput.append(calculateF(TYPES.PRECISION_1,TYPES.RECALL_1,evaluators,PRECISION_1K,query_total_relavent_docs) + "\t");
			case F_5:
				evalOutput.append(calculateF(TYPES.PRECISION_5,TYPES.RECALL_5,evaluators,PRECISION_5K,query_total_relavent_docs) + "\t");
			case F_10:
				evalOutput.append(calculateF(TYPES.PRECISION_10,TYPES.RECALL_10,evaluators,PRECISION_10K,query_total_relavent_docs) + "\t");
			case PRECISIONATRECALLPOINTS:
				HashMap<Double,Double> precisonAtRecallPoints = calculateRecall(F_all_points);
				ArrayList<Double> keys = new ArrayList<Double>(precisonAtRecallPoints.keySet());
				Collections.sort(keys);
				for(Double key: keys){
					evalOutput.append(precisonAtRecallPoints.get(key) + "\t");
				}
			case AVG:
				evalOutput.append(calculateAvg(Avg_Sum, query_total_relavent_docs) + "\t");
			case NDCG_1:
				DCG = calculateDCG(1,topRelevantDocsGain);
				sortedtopRelevantDocsGain = new ArrayList<Double>(topRelevantDocsGain);
				Collections.sort(sortedtopRelevantDocsGain,Collections.reverseOrder());
				idealDCG = calculateDCG(1,sortedtopRelevantDocsGain);
				if(query_total_relavent_docs == 0){
					evalOutput.append(0.0 + "\t");
				}
				else{
					evalOutput.append(DCG/idealDCG + "\t");
				}
			case NDCG_5:
				DCG = calculateDCG(5,topRelevantDocsGain);
				sortedtopRelevantDocsGain = new ArrayList<Double>(topRelevantDocsGain);
				Collections.sort(sortedtopRelevantDocsGain,Collections.reverseOrder());
				idealDCG = calculateDCG(5,sortedtopRelevantDocsGain);
				if(query_total_relavent_docs == 0){
					evalOutput.append(0.0 + "\t");
				}
				else{
					evalOutput.append(DCG/idealDCG + "\t");
				}
			case NDCG_10:
				DCG = calculateDCG(10,topRelevantDocsGain);
				sortedtopRelevantDocsGain = new ArrayList<Double>(topRelevantDocsGain);
				Collections.sort(sortedtopRelevantDocsGain,Collections.reverseOrder());
				idealDCG = calculateDCG(10,sortedtopRelevantDocsGain);
				if(query_total_relavent_docs == 0){
					evalOutput.append(0.0 + "\t");
				}
				else{
					evalOutput.append(DCG/idealDCG + "\t");
				}
			case RECIPROCALRANK:
				evalOutput.append(reciprocalRank + "\t");
			default:
				break;
			}
		System.out.println(evalOutput);
		}

	private static Double calculatePrecion(TYPES precision1,
			HashMap<TYPES, Double> evaluators, int precision) {
		return (evaluators.get(precision1)/precision);
	}

	private static Double calculateRecall(TYPES recall1,
			HashMap<TYPES, Double> evaluators, int query_total_relavent_docs) {
			if(query_total_relavent_docs == 0){
				return 0.0;
			}
		return (evaluators.get(recall1)/query_total_relavent_docs);
	}

	private static Double calculateF(TYPES precision1, TYPES recall1,
			HashMap<TYPES, Double> evaluators, int precision, int query_total_relavent_docs){
		if(query_total_relavent_docs == 0){
			return 0.0;
		}
		return new Double(1)/(
				(alpha*(precision/evaluators.get(precision1))) + 
				(alpha*(query_total_relavent_docs/evaluators.get(recall1))));
	}

	private static void updateType1Map(HashMap < TYPES , Double > evaluators){
		evaluators.put(TYPES.PRECISION_1, evaluators.get(TYPES.PRECISION_1) + 1);
		evaluators.put(TYPES.RECALL_1, evaluators.get(TYPES.RECALL_1) + 1);
		evaluators.put(TYPES.F_1, evaluators.get(TYPES.F_1) + 1);
	}

	private static void updateType5Map(HashMap < TYPES , Double > evaluators){
		evaluators.put(TYPES.PRECISION_5, evaluators.get(TYPES.PRECISION_5) + 1);
		evaluators.put(TYPES.RECALL_5, evaluators.get(TYPES.RECALL_5) + 1);
		evaluators.put(TYPES.F_5, evaluators.get(TYPES.F_5) + 1);
	}

	private static void updateType10Map(HashMap < TYPES , Double > evaluators){
		evaluators.put(TYPES.PRECISION_10, evaluators.get(TYPES.PRECISION_10) + 1);
		evaluators.put(TYPES.RECALL_10, evaluators.get(TYPES.RECALL_10) + 1);
		evaluators.put(TYPES.F_10, evaluators.get(TYPES.F_10) + 1);
	}

	private static boolean isRelevant(HashMap < Integer , Double > qr,int did){
		if (qr != null && qr.containsKey(did) && qr.get(did) != 0){
			return true;
		}
		return false;
	}

	private static HashMap<Double,Double> calculateRecall(HashMap<Double,Double> recallMap){
		HashMap<Double,Double> recallPoints = new HashMap<Double,Double>(10);
		Double precision = null;
		for(int i=1;i<=10;i++){
			Double recallPoint = new Double(i/10.0);
			precision = new Double(0);
			for(Double key: recallMap.keySet()){
				if(recallPoint<key && precision < recallMap.get(key)){
					precision = recallMap.get(key);
				}
			}
			recallPoints.put(recallPoint, precision);
		}
		return recallPoints;
	}

	private static Double calculateDCG(int dcgAt, ArrayList<Double> gain){
		Double DCG = new Double(gain.get(0));
		for(int i = 1; i < dcgAt;i++){
			DCG = DCG + (gain.get(i)/(Math.log(i+1)/Math.log(2)));
		}
		return DCG;
	}

	private static Double calculateAvg(double AvgSum, double total_rel_docs){
		if(total_rel_docs == 0){
			return 0.0;
		}
		return (AvgSum/total_rel_docs);
	}

	/*private static Double calculateNDCG(int dcgAt, ArrayList<Double> gain){
		Double DCG = new Double(gain.get(0));
		for(int i = 1; i < dcgAt;i++){
			DCG = DCG + (gain.get(i)/Math.log(i+1));
		}
		return DCG;
	}*/
}