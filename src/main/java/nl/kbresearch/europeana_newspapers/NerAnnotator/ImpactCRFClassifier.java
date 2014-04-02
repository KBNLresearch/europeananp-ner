package nl.kbresearch.europeana_newspapers.NerAnnotator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import nl.kbresearch.europeana_newspapers.NerAnnotator.SpelVarFactory;
import nl.kbresearch.europeana_newspapers.NerAnnotator.utils.NERTStringUtils;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.AcquisitionsPrior;
import edu.stanford.nlp.ie.EmpiricalNERPrior;
import edu.stanford.nlp.ie.EmpiricalNERPriorBIO;
import edu.stanford.nlp.ie.EntityCachingAbstractSequencePrior;
import edu.stanford.nlp.ie.EntityCachingAbstractSequencePriorBIO;
import edu.stanford.nlp.ie.SeminarsPrior;
import edu.stanford.nlp.ie.UniformPrior;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifierEvaluator;
import edu.stanford.nlp.ie.crf.CRFCliqueTree;
import edu.stanford.nlp.ie.crf.CRFDatum;
import edu.stanford.nlp.ie.crf.CRFFeatureExporter;
import edu.stanford.nlp.ie.crf.CRFLabel;
import edu.stanford.nlp.ie.crf.CRFLogConditionalObjectiveFloatFunction;
import edu.stanford.nlp.ie.crf.CRFLogConditionalObjectiveFunction;
import edu.stanford.nlp.ie.crf.CRFLogConditionalObjectiveFunctionForLOP;
import edu.stanford.nlp.ie.crf.CRFNonLinearLogConditionalObjectiveFunction;
import edu.stanford.nlp.ie.crf.CRFNonLinearSecondOrderLogConditionalObjectiveFunction;
import edu.stanford.nlp.ie.crf.CliquePotentialFunction;
import edu.stanford.nlp.ie.crf.FactorTable;
import edu.stanford.nlp.ie.crf.HasCliquePotentialFunction;
import edu.stanford.nlp.ie.crf.LinearCliquePotentialFunction;
import edu.stanford.nlp.ie.crf.NonLinearCliquePotentialFunction;
import edu.stanford.nlp.ie.crf.NonLinearSecondOrderCliquePotentialFunction;
import edu.stanford.nlp.ie.crf.CRFClassifier.TestSequenceModel;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.GoldAnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ShapeAnnotation;
import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.objectbank.ObjectBank;
import edu.stanford.nlp.objectbank.ResettableReaderIteratorFactory;
import edu.stanford.nlp.optimization.AbstractCachingDiffFunction;
import edu.stanford.nlp.optimization.DiffFunction;
import edu.stanford.nlp.optimization.Evaluator;
import edu.stanford.nlp.optimization.FloatFunction;
import edu.stanford.nlp.optimization.Function;
import edu.stanford.nlp.optimization.HasEvaluators;
import edu.stanford.nlp.optimization.HybridMinimizer;
import edu.stanford.nlp.optimization.MemoryEvaluator;
import edu.stanford.nlp.optimization.Minimizer;
import edu.stanford.nlp.optimization.QNMinimizer;
import edu.stanford.nlp.optimization.ResultStoringFloatMonitor;
import edu.stanford.nlp.optimization.ResultStoringMonitor;
import edu.stanford.nlp.optimization.SGDMinimizer;
import edu.stanford.nlp.optimization.SGDToQNMinimizer;
import edu.stanford.nlp.optimization.SGDWithAdaGradAndFOBOS;
import edu.stanford.nlp.optimization.SMDMinimizer;
import edu.stanford.nlp.optimization.ScaledSGDMinimizer;
import edu.stanford.nlp.optimization.StochasticDiffFunctionTester;
import edu.stanford.nlp.optimization.StochasticInPlaceMinimizer;
import edu.stanford.nlp.sequences.BeamBestSequenceFinder;
import edu.stanford.nlp.sequences.BestSequenceFinder;
import edu.stanford.nlp.sequences.Clique;
import edu.stanford.nlp.sequences.CoolingSchedule;
import edu.stanford.nlp.sequences.DocumentReaderAndWriter;
import edu.stanford.nlp.sequences.ExactBestSequenceFinder;
import edu.stanford.nlp.sequences.FactoredSequenceListener;
import edu.stanford.nlp.sequences.FactoredSequenceModel;
import edu.stanford.nlp.sequences.FeatureFactory;
import edu.stanford.nlp.sequences.ObjectBankWrapper;
import edu.stanford.nlp.sequences.PlainTextDocumentReaderAndWriter;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import edu.stanford.nlp.sequences.SequenceGibbsSampler;
import edu.stanford.nlp.sequences.SequenceListener;
import edu.stanford.nlp.sequences.SequenceModel;
import edu.stanford.nlp.util.ArrayUtils;
import edu.stanford.nlp.util.ConvertByteArray;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.HashIndex;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.PaddedList;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Quadruple;
import edu.stanford.nlp.util.ReflectionLoading;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.Timing;
import edu.stanford.nlp.util.Triple;

public class ImpactCRFClassifier<IN extends CoreMap> extends CRFClassifier<IN> {


    /* Additions for the spelvar module */
    SpelVarFactory sv = new SpelVarFactory();
    static boolean useSpelVar = false;

    // special for KB ******************************
    ConcurrentHashMap<String, String> svPropsHM;
    // special for KB ******************************

    private  List<String> gazettes = new ArrayList<String>();
    private  List<String> spelvarlist = new ArrayList<String>();

    // these maps store the testfile's answers
    static TreeMap<String, Integer> NEListHM = new TreeMap<String, Integer>();
    static TreeMap<Integer, String> textToXML = new TreeMap<Integer, String>();
    static int storeTextCounter = 0;
    static String meta1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?xml-stylesheet type=\"text/css\" href=\"styleNERT.css\"?>\n<FILE>\n\t<FILEINFO>\n\t\t<SOURCEFILE>NER-results ";
    static String meta2 = "</SOURCEFILE>\n\t\t<DATE>";
    static String meta3 = "\t\t</DATE>\n\t</FILEINFO>\n";
    static List<String> usedTags = new ArrayList<String>();

    String trainFormat = "bio";

    public ConcurrentHashMap<Integer, String> labeledSentences = new ConcurrentHashMap<Integer, String>();

    List<Index<CRFLabel>> labelIndices;
    Pair<double[][], double[][]> entityMatrices;
    Index<String> tagIndex;

    /** Parameter weights of the classifier. */
    double[][] weights;
    double[][] linearWeights;
    double[][] inputLayerWeights4Edge;
    double[][] outputLayerWeights4Edge;
    double[][] inputLayerWeights;
    double[][] outputLayerWeights;

    Index<String> featureIndex;
    /** caches the featureIndex */
    int[] map;
    List<Set<Integer>> featureIndicesSetArray;
    List<List<Integer>> featureIndicesListArray;
    Random random = new Random(2147483647L);
    Index<Integer> nodeFeatureIndicesMap;
    Index<Integer> edgeFeatureIndicesMap;
    Map<String, double[]> embeddings; // = null;
    Minimizer minimizer;

    CliquePotentialFunction cliquePotentialFunction;
    HasCliquePotentialFunction cliquePotentialFunctionHelper;


    /**
     * Name of default serialized classifier resource to look for in a jar file.
     */
    public static final String DEFAULT_CLASSIFIER = "ner-eng-ie.crf-3-all2008.ser.gz";
    private static final boolean VERBOSE = false;


    /**
     * Fields for grouping features
     */
    Pattern suffixPatt = Pattern.compile(".+?((?:-[A-Z]+)+)\\|.*C");
    Index<String> templateGroupIndex;
    Map<Integer, Integer> featureIndexToTemplateIndex;



    // List selftraindatums = new ArrayList();

    public ImpactCRFClassifier() {
        super(new SeqClassifierFlags());
    }

    public ImpactCRFClassifier(Properties props) {
        super(props);
    }

    /**
     * Makes a copy of the crf classifier
     */
    public ImpactCRFClassifier(ImpactCRFClassifier<IN> crf) {
        super(crf.flags);
        this.windowSize = crf.windowSize;
        this.featureFactory = crf.featureFactory;
        this.pad = crf.pad;
        this.knownLCWords = (crf.knownLCWords != null) ? Generics.<String>newHashSet(crf.knownLCWords) : null;
        this.featureIndex = (crf.featureIndex != null) ? new HashIndex<String>(crf.featureIndex.objectsList()) : null;
        if (crf.flags.nonLinearCRF) {
            this.nodeFeatureIndicesMap = (crf.nodeFeatureIndicesMap != null) ? new HashIndex<Integer>(crf.nodeFeatureIndicesMap.objectsList()) : null;
            this.edgeFeatureIndicesMap = (crf.edgeFeatureIndicesMap != null) ? new HashIndex<Integer>(crf.edgeFeatureIndicesMap.objectsList()) : null;
        }
        this.classIndex = (crf.classIndex != null) ? new HashIndex<String>(crf.classIndex.objectsList()) : null;
        if (crf.labelIndices != null) {
            this.labelIndices = new ArrayList<Index<CRFLabel>>(crf.labelIndices.size());
            for (int i = 0; i < crf.labelIndices.size(); i++) {
                this.labelIndices.add((crf.labelIndices.get(i) != null) ? new HashIndex<CRFLabel>(crf.labelIndices.get(i).objectsList()) : null);
            }
        } else {
            this.labelIndices = null;
        }
        this.cliquePotentialFunction = crf.cliquePotentialFunction;

    }




    public synchronized void dropFeaturesBelowThreshold(double threshold) {
        Index<String> newFeatureIndex = new HashIndex<String>();
        for (int i = 0; i < weights.length; i++) {
            double smallest = weights[i][0];
            double biggest = weights[i][0];
            for (int j = 1; j < weights[i].length; j++) {
                if (weights[i][j] > biggest) {
                    biggest = weights[i][j];
                }
                if (weights[i][j] < smallest) {
                    smallest = weights[i][j];
                }
                if (biggest - smallest > threshold) {
                    newFeatureIndex.add(featureIndex.get(i));
                    break;
                }
            }
        }

        int[] newMap = new int[newFeatureIndex.size()];
        for (int i = 0; i < newMap.length; i++) {
            int index = featureIndex.indexOf(newFeatureIndex.get(i));
            newMap[i] = map[index];
        }
        map = newMap;
        featureIndex = newFeatureIndex;
    }


    /**
     * Convert a document List into arrays storing the data features and labels.
     * This is used at test time.
     *
     * @param document Testing documents
     * @return A Triple, where the first element is an int[][][] representing the
     *         data, the second element is an int[] representing the labels, and
     *         the third element is a double[][][] representing the feature values (optionally null)
     */

    // new 2013
    public synchronized Triple<int[][][], int[], double[][][]> documentToDataAndLabels(List<IN> document) {
        return documentToDataAndLabels(document, false);
    }



    /**
     * Convert a document List into arrays storing the data features and labels.
     * This is used at both training and test time.
     *
     * @param document Training documents
     * @return A Triple, where the first element is an int[][][] representing the
     *         data, the second element is an int[] representing the labels, the third
     *         element is an (optional, could be null) double[][][] representing the
     *         feature real values.
     */

    // modified 2013
    public synchronized Triple<int[][][], int[], double[][][]> documentToDataAndLabels(
            List<IN> document, boolean trainTime) {

        boolean droppedFeature = false; // todo: remove me
        int docSize = document.size();
        // first index is position in the document also the index of the
        // clique/factor table
        // second index is the number of elements in the clique/window these
        // features are for (starting with last element)
        // third index is position of the feature in the array that holds them.
        // An element in data[j][k][m] is the feature index of the mth feature occurring in
        // position k of the jth clique
        int[][][] data = new int[docSize][windowSize][];
        double[][][] featureVals = new double[docSize][windowSize][];
        // index is the position in the document.
        // element in labels[j] is the index of the correct label (if it exists) at
        // position j of document
        int[] labels = new int[docSize];

        if (flags.useReverse) {
            Collections.reverse(document);
        }

        /******************************************************************************************/

		/* Start of first spelvarmodule insertion */

		/*
		 * Look for variant => word pairs in the testfile. Track which
		 * adjustments have been made to the original sentence; these are turned
		 * back at the end of the loop.
		 */

        String t1 = "";
        if (useSpelVar) {
            document = sv.applySpelVarRulesToDocument(document, knownLCWords);
        }
		
		/* End of first spelvarmodule insertion */

        /******************************************************************************************/

        for (int j = 0; j < docSize; j++) {
            CRFDatum<List<String>, CRFLabel> d = makeDatum(document, j, featureFactory);

            List<List<String>> features = d.asFeatures();
            List<double[]> featureValList = d.asFeatureVals();
            for (int k = 0, fSize = features.size(); k < fSize; k++) {
                Collection<String> cliqueFeatures = features.get(k);
                data[j][k] = new int[cliqueFeatures.size()];
                if(featureValList != null) { // CRFBiasedClassifier.makeDatum causes null
                    featureVals[j][k] = featureValList.get(k);
                }
                int m = 0;
                for (String feature : cliqueFeatures) {
                    int index = featureIndex.indexOf(feature);
                    if (index >= 0) {
                        data[j][k][m] = index;
                        m++;
                    } else {
                        // this is where we end up when we do feature threshold cutoffs
                    }
                }

                if (m < data[j][k].length) {
                    int[] f = new int[m];
                    System.arraycopy(data[j][k], 0, f, 0, m);
                    data[j][k] = f;
                    if (featureVals[j][k] != null) {
                        double[] fVal = new double[m];
                        System.arraycopy(featureVals[j][k], 0, fVal, 0, m);
                        featureVals[j][k] = fVal;
                    }
                }
            }

            IN wi = document.get(j);
            labels[j] = classIndex.indexOf(wi.get(CoreAnnotations.AnswerAnnotation.class));
        }


        // write the answers to the NElist
        storeAnswers(document);

        /******************************************************************************************/

		/* Start of second spelvarmodule insertion */

		/* Put the adjusted words back in their original form */

        if (useSpelVar) {
            document = sv.resetSpelVarChanges(document);
        }

        if (flags.useReverse) {
            Collections.reverse(document);
        }

        if (flags.nonLinearCRF) {
            data = transformDocData(data);
        }

		/* End of second spelvarmodule insertion */

        /******************************************************************************************/

        return new Triple<int[][][], int[], double[][][]>(data, labels, featureVals);
    }



    private void storeAnswers(List<IN> document) {
        String tempTag = "";
        String tempWord = "";
        String tempO = "";
        boolean foundNE = false;
        boolean foundO = false;
        for (IN fl : document) {
            String answer = fl.get(AnswerAnnotation.class);
            String goldAnswer = fl.get(GoldAnswerAnnotation.class);

            if (answer != null) {

                // String sub1 = answer.substring(0,1);
                // String sub2 = answer.substring(answer.length()-3,
                // answer.length());

                if (	( foundNE && answer.substring(0, 1).equals("O") )
                        ||
                        ( foundNE && answer.substring(0, 1).equals("B") )
                        ) {
                    storeNEs(tempWord, tempTag);
                    storeText(tempWord, tempTag);
                    tempWord = "";
                    tempTag = "";
                    foundNE = false;
                }
                if ((foundO) && (answer.substring(0, 1).equals("O"))) {
                    tempO += " " + ((CoreLabel) fl).word();
                }
                if ((!foundO) && (answer.substring(0, 1).equals("O"))) {
                    tempO = ((CoreLabel) fl).word();
                    foundO = true;
                }
                if ((!foundNE) && (answer.substring(0, 1).equals("B"))) {
                    if (foundO) {
                        storeText(tempO, "O");
                        tempO = "";
                        foundO = false;
                    }
                    tempTag = answer.substring(answer.length() - 3, answer
                            .length());
                    tempWord = ((CoreLabel) fl).word();
                    foundNE = true;
                }
                if ((foundNE) && (answer.substring(0, 1).equals("I"))) {
                    tempWord += " " + ((CoreLabel) fl).word();
                }
            }

			/* End of sentence could have been reached during an O-chunk */

            if (foundO) {
                // tempO += "\n";
                storeText(tempO, "O");
                tempO = "";
                foundO = false;
            }
            storeText("X", "X");
        }
    }


    // new 2013
    private int[][][] transformDocData(int[][][] docData) {
        int[][][] transData = new int[docData.length][][];
        for (int i = 0; i < docData.length; i++) {
            transData[i] = new int[docData[i].length][];
            for (int j = 0; j < docData[i].length; j++) {
                int[] cliqueFeatures = docData[i][j];
                transData[i][j] = new int[cliqueFeatures.length];
                for (int n = 0; n < cliqueFeatures.length; n++) {
                    int transFeatureIndex = -1;
                    if (j == 0) {
                        transFeatureIndex = nodeFeatureIndicesMap.indexOf(cliqueFeatures[n]);
                        if (transFeatureIndex == -1)
                            throw new RuntimeException("node cliqueFeatures[n]="+cliqueFeatures[n]+" not found, nodeFeatureIndicesMap.size="+nodeFeatureIndicesMap.size());
                    } else {
                        transFeatureIndex = edgeFeatureIndicesMap.indexOf(cliqueFeatures[n]);
                        if (transFeatureIndex == -1)
                            throw new RuntimeException("edge cliqueFeatures[n]="+cliqueFeatures[n]+" not found, edgeFeatureIndicesMap.size="+edgeFeatureIndicesMap.size());
                    }
                    transData[i][j][n] = transFeatureIndex;
                }
            }
        }
        return transData;
    }




    // new 2013
    public synchronized void printLabelInformation(String testFile, DocumentReaderAndWriter<IN> readerAndWriter) throws Exception {
        ObjectBank<List<IN>> documents = makeObjectBankFromFile(testFile, readerAndWriter);
        for (List<IN> document : documents) {
            printLabelValue(document);
        }
    }

    public synchronized void printLabelValue(List<IN> document) {

        if (flags.useReverse) {
            Collections.reverse(document);
        }

        NumberFormat nf = new DecimalFormat();

        List<String> classes = new ArrayList<String>();
        for (int i = 0; i < classIndex.size(); i++) {
            classes.add(classIndex.get(i));
        }
        String[] columnHeaders = classes.toArray(new String[classes.size()]);

        for (int j = 0; j < document.size(); j++) {

            System.out.println("--== "
                    + ((CoreLabel) document.get(j)).word() + " ==--");

            List<String[]> lines = new ArrayList<String[]>();
            List<String> rowHeaders = new ArrayList<String>();
            List<String> line = new ArrayList<String>();

            for (int p = 0; p < labelIndices.size(); p++) {
                if (j + p >= document.size()) {
                    continue;
                }
                CRFDatum d = makeDatum(document, j + p, featureFactory);

                List features = d.asFeatures();
                for (int k = p, fSize = features.size(); k < fSize; k++) {
                    Collection<String> cliqueFeatures = (Collection<String>) features
                            .get(k);
                    for (String feature : cliqueFeatures) {
                        int index = featureIndex.indexOf(feature);
                        if (index >= 0) {
                            // line.add(feature+"["+(-p)+"]");
                            rowHeaders.add(feature + '[' + (-p) + ']');
                            double[] values = new double[labelIndices.get(0).size()];
                            for (CRFLabel label : labelIndices.get(k)) {
                                int[] l = label.getLabel();
                                double v = weights[index][labelIndices.get(k)
                                        .indexOf(label)];
                                values[l[l.length - 1 - p]] += v;
                            }
                            for (double value : values) {
                                line.add(nf.format(value));
                            }
                            lines.add(line.toArray(new String[line.size()]));
                            line = new ArrayList<String>();
                        }
                    }
                }
                // lines.add(Collections.<String>emptyList());
                System.out.println(StringUtils.makeAsciiTable(lines
                                .toArray(new String[lines.size()][0]), rowHeaders
                                .toArray(new String[rowHeaders.size()]), columnHeaders,
                        0, 1, true));
                System.out.println();
            }
        }

        if (flags.useReverse) {
            Collections.reverse(document);
        }
    }

    /**
     * Convert an ObjectBank to arrays of data features and labels.
     *
     * @param documents
     * @return A Pair, where the first element is an int[][][][] representing
     *         the data and the second element is an int[][] representing the
     *         labels.
     */
    public synchronized Triple<int[][][][], int[][], double[][][][]> documentsToDataAndLabels(
            Collection<List<IN>> documents) {

        // first index is the number of the document
        // second index is position in the document also the index of the
        // clique/factor table
        // third index is the number of elements in the clique/window these features
        // are for (starting with last element)
        // fourth index is position of the feature in the array that holds them
        // element in data[i][j][k][m] is the index of the mth feature occurring in
        // position k of the jth clique of the ith document
        // int[][][][] data = new int[documentsSize][][][];
        List<int[][][]> data = new ArrayList<int[][][]>();
        List<double[][][]> featureVal = new ArrayList<double[][][]>();

        // first index is the number of the document
        // second index is the position in the document
        // element in labels[i][j] is the index of the correct label (if it exists)
        // at position j in document i
        // int[][] labels = new int[documentsSize][];
        List<int[]> labels = new ArrayList<int[]>();

        int numDatums = 0;

        for (List<IN> doc : documents) {
            Triple<int[][][], int[], double[][][]> docTriple = documentToDataAndLabels(doc, true);
            data.add(docTriple.first());
            labels.add(docTriple.second());
            numDatums += doc.size();
        }

        System.out.println("#numClasses: " + classIndex.size() + " "
                + classIndex);
        System.out.println("#numDocuments: " + data.size());
        System.out.println("#numDatums: " + numDatums);
        System.out.println("#numFeatures: " + featureIndex.size());
        printFeatures();

        double[][][][] featureValArr = null;
        if (flags.useEmbedding)
            featureValArr = featureVal.toArray(new double[data.size()][][][]);


        return new Triple<int[][][][], int[][], double[][][][]>(
                data.toArray(new int[data.size()][][][]),
                labels.toArray(new int[labels.size()][]),
                featureValArr);
    }

    protected void printFeatures() {
        if (flags.printFeatures == null) {
            return;
        }
        try {
            String enc = flags.inputEncoding;
            if (flags.inputEncoding == null) {
                System.err
                        .println("flags.inputEncoding doesn't exist, Use UTF-8 as default");
                enc = "UTF-8";
            }

            PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream("feats-" + flags.printFeatures
                            + ".txt"), enc), true);
            for (int i = 0; i < featureIndex.size(); i++) {
                pw.println(featureIndex.get(i));
            }
            pw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * This routine builds the <code>labelIndices</code> which give the
     * empirically legal label sequences (of length (order) at most
     * <code>windowSize</code>) and the <code>classIndex</code>, which indexes
     * known answer classes.
     *
     * @param ob
     *            The training data: Read from an ObjectBank, each item in it is
     *            a List<CoreLabel>.
     */
    protected void makeAnswerArraysAndTagIndex(Collection<List<IN>> ob) {

        Set<String>[] featureIndices = new HashSet[windowSize];
        for (int i = 0; i < windowSize; i++) {
            featureIndices[i] = Generics.newHashSet();
        }

        labelIndices = new ArrayList<Index<CRFLabel>>(windowSize);
        for (int i = 0; i < windowSize; i++) {
            labelIndices.add(new HashIndex<CRFLabel>());
        }

        Index<CRFLabel> labelIndex = labelIndices.get(windowSize - 1);

        classIndex = new HashIndex<String>();
        // classIndex.add("O");
        classIndex.add(flags.backgroundSymbol);

        HashSet[] seenBackgroundFeatures = new HashSet[2];
        seenBackgroundFeatures[0] = new HashSet();
        seenBackgroundFeatures[1] = new HashSet();

        int count = 0;
        int wordCount = 0;

        for (List<IN> doc : ob) {

            if (flags.useReverse) {
                Collections.reverse(doc);
            }

            /******************************************************************************************/

			/* Start of third spelvarmodule insertion */

			/*
			 * Look for variant => word pairs in the trainfile. Contrary to the
			 * testfile, adjustments made are not turned back (since the
			 * trainfile is not printed).
			 */

            if (useSpelVar) {
                sv.findVariants(doc, doc.size(), knownLCWords);
            }

			/* End of third spelvarmodule insertion */

            /******************************************************************************************/

            // create the full set of labels in classIndex
            // note: update to use addAll later
            for (IN token : doc) {
                wordCount++;
                String ans = token.get(CoreAnnotations.AnswerAnnotation.class);
                if (ans == null || ans.equals("")) {
                    throw new IllegalArgumentException("Word " + wordCount + " (\"" + token.get(CoreAnnotations.TextAnnotation.class) + "\") has a blank answer");
                }
                classIndex.add(ans);
            }


            for (int j = 0, docSize = doc.size(); j < docSize; j++) {
                CRFDatum<List<String>, CRFLabel> d = makeDatum(doc, j, featureFactory);
                labelIndex.add(d.label());

                List<List<String>> features = d.asFeatures();
                for (int k = 0, fSize = features.size(); k < fSize; k++) {
                    Collection<String> cliqueFeatures = features.get(k);
                    if (k < 2 && flags.removeBackgroundSingletonFeatures) {
                        String ans = doc.get(j).get(CoreAnnotations.AnswerAnnotation.class);
                        boolean background = ans.equals(flags.backgroundSymbol);
                        if (k == 1 && j > 0 && background) {
                            ans = doc.get(j - 1).get(CoreAnnotations.AnswerAnnotation.class);
                            background = ans.equals(flags.backgroundSymbol);
                        }
                        if (background) {
                            for (String f : cliqueFeatures) {
                                if (!featureIndices[k].contains(f)) {
                                    if (seenBackgroundFeatures[k].contains(f)) {
                                        seenBackgroundFeatures[k].remove(f);
                                        featureIndices[k].add(f);
                                    } else {
                                        seenBackgroundFeatures[k].add(f);
                                    }
                                }
                            }
                        } else {
                            seenBackgroundFeatures[k].removeAll(cliqueFeatures);
                            featureIndices[k].addAll(cliqueFeatures);
                        }
                    } else {
                        featureIndices[k].addAll(cliqueFeatures);
                    }
                }
            }

            if (flags.useReverse) {
                Collections.reverse(doc);
            }
            count++;

        }

        // String[] fs = new String[featureIndices[0].size()];
        // for (Iterator iter = featureIndices[0].iterator(); iter.hasNext(); )
        // {
        // System.out.println(iter.next());
        // }

        int numFeatures = 0;
        for (int i = 0; i < windowSize; i++) {
            numFeatures += featureIndices[i].size();
        }

        featureIndex = new HashIndex<String>();
        map = new int[numFeatures];

        if (flags.groupByFeatureTemplate) {
            templateGroupIndex = new HashIndex<String>();
            featureIndexToTemplateIndex = new ConcurrentHashMap<Integer, Integer>();
        }

        Matcher m  = null;
        String groupSuffix = null;
        for (int i = 0; i < windowSize; i++) {
            Index<Integer> featureIndexMap = new HashIndex<Integer>();

            featureIndex.addAll(featureIndices[i]);
            for (String str : featureIndices[i]) {
                int index = featureIndex.indexOf(str);
                map[index] = i;
                featureIndexMap.add(index);

                // grouping features by template
                if (flags.groupByFeatureTemplate) {
                    m = suffixPatt.matcher(str);
                    groupSuffix = "NoTemplate";
                    if (m.matches()) {
                        groupSuffix = m.group(1);
                    }
                    groupSuffix += "-c:"+i;

                    int groupIndex = templateGroupIndex.indexOf(groupSuffix, true);
                    featureIndexToTemplateIndex.put(index, groupIndex);
                }
            }
            if (i == 0) {
                nodeFeatureIndicesMap = featureIndexMap;
                System.out.println("setting nodeFeatureIndicesMap, size="+nodeFeatureIndicesMap.size());
            } else {
                edgeFeatureIndicesMap = featureIndexMap;
                System.out.println("setting edgeFeatureIndicesMap, size="+edgeFeatureIndicesMap.size());
            }
        }

        if (flags.numOfFeatureSlices > 0) {
            System.out.println("Taking " + flags.numOfFeatureSlices + " out of " + flags.totalFeatureSlice + " slices of node features for training");
            pruneNodeFeatureIndices(flags.totalFeatureSlice, flags.numOfFeatureSlices);
        }

        if (flags.useObservedSequencesOnly) {
            for (int i = 0, liSize = labelIndex.size(); i < liSize; i++) {
                CRFLabel label = labelIndex.get(i);
                for (int j = windowSize - 2; j >= 0; j--) {
                    label = label.getOneSmallerLabel();
                    labelIndices.get(j).add(label);
                }
            }
        } else {
            for (int i = 0; i < labelIndices.size(); i++) {
                labelIndices.set(i, allLabels(i + 1, classIndex));
            }
        }

        if (VERBOSE) {
            for (int i = 0, fiSize = featureIndex.size(); i < fiSize; i++) {
                System.out.println(i + ": " + featureIndex.get(i));
            }
        }
    }

    protected static Index<CRFLabel> allLabels(int window, Index classIndex) {
        int[] label = new int[window];
        // cdm july 2005: below array initialization isn't necessary: JLS (3rd
        // ed.) 4.12.5
        // Arrays.fill(label, 0);
        int numClasses = classIndex.size();
        Index<CRFLabel> labelIndex = new HashIndex<CRFLabel>();
        OUTER: while (true) {
            CRFLabel l = new CRFLabel(label);
            labelIndex.add(l);
            int[] label1 = new int[window];
            System.arraycopy(label, 0, label1, 0, label.length);
            label = label1;
            for (int j = 0; j < label.length; j++) {
                label[j]++;
                if (label[j] >= numClasses) {
                    label[j] = 0;
                    if (j == label.length - 1) {
                        break OUTER;
                    }
                } else {
                    break;
                }
            }
        }
        return labelIndex;
    }

    // new 2013
    protected void pruneNodeFeatureIndices(int totalNumOfFeatureSlices, int numOfFeatureSlices) {
        int numOfNodeFeatures = nodeFeatureIndicesMap.size();
        int beginIndex = 0;
        int endIndex = Math.min( (int)(numOfNodeFeatures / (totalNumOfFeatureSlices+0.0) * numOfFeatureSlices), numOfNodeFeatures);
        List<Integer> nodeFeatureOriginalIndices = nodeFeatureIndicesMap.objectsList();
        List<Integer> edgeFeatureOriginalIndices = edgeFeatureIndicesMap.objectsList();

        Index<Integer> newNodeFeatureIndex = new HashIndex<Integer>();
        Index<Integer> newEdgeFeatureIndex = new HashIndex<Integer>();
        Index<String> newFeatureIndex = new HashIndex<String>();

        for (int i = beginIndex; i < endIndex; i++) {
            int oldIndex = nodeFeatureOriginalIndices.get(i);
            String f = featureIndex.get(oldIndex);
            int index = newFeatureIndex.indexOf(f, true);
            newNodeFeatureIndex.add(index);
        }
        for (Integer edgeFIndex: edgeFeatureOriginalIndices) {
            String f = featureIndex.get(edgeFIndex);
            int index = newFeatureIndex.indexOf(f, true);
            newEdgeFeatureIndex.add(index);
        }

        nodeFeatureIndicesMap = newNodeFeatureIndex;
        edgeFeatureIndicesMap = newEdgeFeatureIndex;

        int[] newMap = new int[newFeatureIndex.size()];
        for (int i = 0; i < newMap.length; i++) {
            int index = featureIndex.indexOf(newFeatureIndex.get(i));
            newMap[i] = map[index];
        }
        map = newMap;

        featureIndex = newFeatureIndex;
    }




    /**
     * Makes a CRFDatum by producing features and a label from input data at a
     * specific position, using the provided factory.
     *
     * @param info
     *            The input data
     * @param loc
     *            The position to build a datum at
     * @param featureFactory
     *            The FeatureFactory to use to extract features
     * @return The constructed CRFDatum
     */
    public synchronized CRFDatum<List<String>, CRFLabel> makeDatum(
            List<IN> info, int loc,
            edu.stanford.nlp.sequences.FeatureFactory<IN> featureFactory) {
        //pad.set(AnswerAnnotation.class, flags.backgroundSymbol);
        PaddedList<IN> pInfo = new PaddedList<IN>(info, pad);

        ArrayList<List<String>> features = new ArrayList<List<String>>();
        List<double[]> featureVals = new ArrayList<double[]>();

        // for (int i = 0; i < windowSize; i++) {
        // List featuresC = new ArrayList();
        // for (int j = 0; j < FeatureFactory.win[i].length; j++) {
        // featuresC.addAll(featureFactory.features(info, loc,
        // FeatureFactory.win[i][j]));
        // }
        // features.add(featuresC);
        // }

        Collection<Clique> done = Generics.newHashSet();
        for (int i = 0; i < windowSize; i++) {
            List featuresC = new ArrayList<String>();
            Collection<Clique> windowCliques = FeatureFactory.getCliques(i, 0);
            windowCliques.removeAll(done);
            done.addAll(windowCliques);
            double[] featureValArr = null;

            for (Clique c : windowCliques) {
                featuresC.addAll(featureFactory
                        .getCliqueFeatures(pInfo, loc, c));

            }
            features.add(featuresC);
            featureVals.add(featureValArr);
        }

        int[] labels = new int[windowSize];

        for (int i = 0; i < windowSize; i++) {
            String answer = pInfo.get(loc + i - windowSize + 1).get(
                    CoreAnnotations.AnswerAnnotation.class);
            labels[i] = classIndex.indexOf(answer);
        }

        printFeatureLists(pInfo.get(loc), features);

        CRFDatum<List<String>, CRFLabel> d = new CRFDatum<List<String>, CRFLabel>(features, new CRFLabel(labels), featureVals);
        // System.out.println(d);
        return d;
    }

    public static class TestSequenceModel implements SequenceModel {

        private int window;
        private int numClasses;
        // private FactorTable[] factorTables;
        private CRFCliqueTree cliqueTree;
        private int[] tags;
        private int[] backgroundTag;

        // public Scorer(FactorTable[] factorTables) {
        public TestSequenceModel(CRFCliqueTree cliqueTree) {
            // this.factorTables = factorTables;
            this.cliqueTree = cliqueTree;
            // this.window = factorTables[0].windowSize();
            this.window = cliqueTree.window();
            // this.numClasses = factorTables[0].numClasses();
            this.numClasses = cliqueTree.getNumClasses();
            tags = new int[numClasses];
            for (int i = 0; i < tags.length; i++) {
                tags[i] = i;
            }
            backgroundTag = new int[] { cliqueTree.backgroundIndex() };
        }

        public synchronized int length() {
            return cliqueTree.length();
        }

        public synchronized int leftWindow() {
            return window - 1;
        }

        public synchronized int rightWindow() {
            return 0;
        }

        public synchronized int[] getPossibleValues(int pos) {
            if (pos < window - 1) {
                return backgroundTag;
            }
            return tags;
        }

        public synchronized double scoreOf(int[] tags, int pos) {
            int[] previous = new int[window - 1];
            int realPos = pos - window + 1;
            for (int i = 0; i < window - 1; i++) {
                previous[i] = tags[realPos + i];
            }
            return cliqueTree.condLogProbGivenPrevious(realPos, tags[pos],
                    previous);
        }

        public synchronized double[] scoresOf(int[] tags, int pos) {
            int realPos = pos - window + 1;
            double[] scores = new double[numClasses];
            int[] previous = new int[window - 1];
            for (int i = 0; i < window - 1; i++) {
                previous[i] = tags[realPos + i];
            }
            for (int i = 0; i < numClasses; i++) {
                scores[i] = cliqueTree.condLogProbGivenPrevious(realPos, i,
                        previous);
            }
            return scores;
        }

        public synchronized double scoreOf(int[] sequence) {
            throw new UnsupportedOperationException();
        }

    } // end class TestSequenceModel

    @Override
    public synchronized List<IN> classify(List<IN> document) {
        if (flags.doGibbs) {
            try {
                return classifyGibbs(document);
            } catch (Exception e) {
                System.err.println("Error running testGibbs inference!");
                e.printStackTrace();
                return null;
            }
        } else if (flags.crfType.equalsIgnoreCase("maxent")) {
            return classifyMaxEnt(document);
        } else {
            throw new RuntimeException("Unsupported inference type: " + flags.crfType);
        }
    }




    /**
     * Write the classifications of the Sequence classifier out to a writer in a
     * format determined by the DocumentReaderAndWriter used.
     *
     * @param doc Documents to write out
     * @param printWriter Writer to use for output
     * @throws IOException If an IO problem
     */
    public synchronized void writeAnswers(List<IN> doc, PrintWriter printWriter,
                                          DocumentReaderAndWriter<IN> readerAndWriter)
            throws IOException {
        if (flags.lowerNewgeneThreshold) {
            return;
        }
        if (flags.numRuns <= 1) {
            readerAndWriter.printAnswers(doc, printWriter);
            // out.println();
            printWriter.flush();
        }
    }

    public synchronized void writeAnswers(String str) throws Exception {
        PrintWriter out;
        if (flags.outputEncoding == null) {
            out = new PrintWriter(System.out, true);
        } else {
            out = new PrintWriter(new OutputStreamWriter(System.out,
                    flags.outputEncoding), true);
        }
        out.print(str);
        out.flush();
    }


    /* 	This is a general method for printing out the results after classifying.
		 * 	The output format determines the output. Note that for xml, only the
		 * 	classified chunks are printed in this method. The parts of the
		 * 	file/string that are not considered are already printed in
		 * 	initExtracting().
		 */
    public synchronized void writeAnswers(List<IN> doc, String inputFormat, String outputFormat, ArrayList<Integer> eol)
            throws Exception {



        PrintWriter out;
        if (flags.outputEncoding == null) {
            out = new PrintWriter(System.out, true);
        } else {
            out = new PrintWriter(new OutputStreamWriter(System.out,
                    flags.outputEncoding), true);
        }
        if(outputFormat.equalsIgnoreCase("xml") || outputFormat.equalsIgnoreCase("txt")){
				
				/*	For txt-format: the eol-list holds all positions 
				 * 	before which a newline has to come. if eol=0, 
				 * 	we start with a newline
				 */

            if(eol.contains(0)){
                out.println("");
            }
            boolean isTag = false;
            String s = "";
            int wordCounter = 0;

            for (IN wi : doc) {
                //s = nl.inl.impact.ner.utils.ConvertTxt2Bio.bio2txt(wi.getString(WordAnnotation.class), wi.getString(GoldAnswerAnnotation.class), wi.getString(AnswerAnnotation.class));
                //System.err.println(wi.getString(WordAnnotation.class)+" "+wi.getString(GoldAnswerAnnotation.class)+" "+wi.getString(AnswerAnnotation.class));
                s += ((CoreLabel) wi).getString(CoreAnnotations.TextAnnotation.class)+" "+((CoreLabel) wi).getString(GoldAnswerAnnotation.class)+" "+((CoreLabel) wi).getString(AnswerAnnotation.class)+"\n";
                wordCounter++;
            }

            //now convert bio > txt
            s = nl.inl.impact.ner.utils.ConvertTxt2Bio.bio2txt(s, eol);
            //System.out.println("sWriteAnswers: >"+s+"<");
            //add a white space after each chunk when we're dealing with text

            out.print(s);
            if(outputFormat.equalsIgnoreCase("txt")){
                out.print(" ");
            }
            //	Add \n when we're printing out bio to xml after each sentence
            if(inputFormat.equalsIgnoreCase("bio") && outputFormat.equalsIgnoreCase("xml")){
                out.println();
            }
            if(inputFormat.equalsIgnoreCase("xml") && outputFormat.equalsIgnoreCase("xml")){
                //out.print(" ");
            }
        }
        else{
            //	Print bio
            for (IN wi : doc) {
                String prev = ((CoreLabel) wi).getString(CoreAnnotations.TextAnnotation.class);
                out.print(prev);
                out.print("\t");
                out.print(((CoreLabel) wi).getString(GoldAnswerAnnotation.class));
                out.print("\t");
                out.print(((CoreLabel) wi).getString(AnswerAnnotation.class));
                out.println();
            }
            //if(inputFormat.equalsIgnoreCase("txt")){
            out.println();
            //}
        }
        //System.out.println("klaar in writeAnswers(List,string, string, arraylist)");
        out.flush();
    }




    @Override
    public synchronized SequenceModel getSequenceModel(List<IN> doc) {
        Triple<int[][][], int[], double[][][]> p = documentToDataAndLabels(doc);
        return getSequenceModel(p);
    }

    private SequenceModel getSequenceModel(Triple<int[][][], int[], double[][][]> documentDataAndLabels) {
        return new TestSequenceModel(getCliqueTree(documentDataAndLabels));
    }

    /**
     * Do standard sequence inference, using either Viterbi or Beam inference
     * depending on the value of {@code flags.inferenceType}.
     *
     * @param document Document to classify. Classification happens in place.
     *          This document is modified.
     * @return The classified document
     */
    public synchronized List<IN> classifyMaxEnt(List<IN> document) {
        if (document.isEmpty()) {
            return document;
        }

        SequenceModel model = getSequenceModel(document);
        return classifyMaxEnt(document, model);
    }

    private List<IN> classifyMaxEnt(List<IN> document, Triple<int[][][], int[], double[][][]> documentDataAndLabels) {
        if (document.isEmpty()) {
            return document;
        }
        SequenceModel model = getSequenceModel(documentDataAndLabels);
        return classifyMaxEnt(document, model);
    }

    private List<IN> classifyMaxEnt(List<IN> document, SequenceModel model) {
        if (document.isEmpty()) {
            return document;
        }

        if (flags.inferenceType == null) {
            flags.inferenceType = "Viterbi";
        }

        BestSequenceFinder tagInference;
        if (flags.inferenceType.equalsIgnoreCase("Viterbi")) {
            tagInference = new ExactBestSequenceFinder();
        } else if (flags.inferenceType.equalsIgnoreCase("Beam")) {
            tagInference = new BeamBestSequenceFinder(flags.beamSize);
        } else {
            throw new RuntimeException("Unknown inference type: " + flags.inferenceType + ". Your options are Viterbi|Beam.");
        }

        int[] bestSequence = tagInference.bestSequence(model);

        if (flags.useReverse) {
            Collections.reverse(document);
        }
        for (int j = 0, docSize = document.size(); j < docSize; j++) {
            IN wi = document.get(j);
            String guess = classIndex.get(bestSequence[j + windowSize - 1]);
            wi.set(CoreAnnotations.AnswerAnnotation.class, guess);
        }
        if (flags.useReverse) {
            Collections.reverse(document);
        }
        return document;
    }

    public synchronized List<IN> classifyGibbs(List<IN> document) throws ClassNotFoundException, SecurityException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Triple<int[][][], int[], double[][][]> p = documentToDataAndLabels(document);
        return classifyGibbs(document, p);
    }

    public synchronized List<IN> classifyGibbs(List<IN> document, Triple<int[][][], int[], double[][][]> documentDataAndLabels)
            throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        // System.err.println("Testing using Gibbs sampling.");
        List<IN> newDocument = document; // reversed if necessary
        if (flags.useReverse) {
            Collections.reverse(document);
            newDocument = new ArrayList<IN>(document);
            Collections.reverse(document);
        }

        CRFCliqueTree<? extends CharSequence> cliqueTree = getCliqueTree(documentDataAndLabels);

        SequenceModel model = cliqueTree;
        SequenceListener listener = cliqueTree;

        SequenceModel priorModel = null;
        SequenceListener priorListener = null;

        if (flags.useNERPrior) {
            EntityCachingAbstractSequencePrior<IN> prior = new EmpiricalNERPrior<IN>(flags.backgroundSymbol, classIndex,
                    newDocument);
            // SamplingNERPrior prior = new SamplingNERPrior(flags.backgroundSymbol,
            // classIndex, newDocument);
            priorModel = prior;
            priorListener = prior;
        } else if (flags.useNERPriorBIO) {
	      /*
	      if (tagIndex == null) {
	        tagIndex = new HashIndex<String>();
	        for (String tag: classIndex.objectsList()) {
	          String[] parts = tag.split("-");
	          if (parts.length > 1)
	            tagIndex.add(parts[parts.length-1]);
	        }
	        tagIndex.add(flags.backgroundSymbol);
	      }
	      if (entityMatrices == null)
	        entityMatrices = BisequenceEmpiricalNERPrior.readEntityMatrices(flags.entityMatrix, tagIndex);
	      */
            EntityCachingAbstractSequencePriorBIO<IN> prior = new EmpiricalNERPriorBIO<IN>(flags.backgroundSymbol, classIndex, tagIndex, newDocument, entityMatrices, flags);
            priorModel = prior;
            priorListener = prior;
        } else if (flags.useAcqPrior) {
            EntityCachingAbstractSequencePrior<IN> prior = new AcquisitionsPrior<IN>(flags.backgroundSymbol, classIndex,
                    newDocument);
            priorModel = prior;
            priorListener = prior;
        } else if (flags.useSemPrior) {
            EntityCachingAbstractSequencePrior<IN> prior = new SeminarsPrior<IN>(flags.backgroundSymbol, classIndex,
                    newDocument);
            priorModel = prior;
            priorListener = prior;
        } else if (flags.useUniformPrior) {
            // System.err.println("Using uniform prior!");
            UniformPrior<IN> uniPrior = new UniformPrior<IN>(flags.backgroundSymbol, classIndex, newDocument);
            priorModel = uniPrior;
            priorListener = uniPrior;
        } else {
            throw new RuntimeException("no prior specified");
        }

        model = new FactoredSequenceModel(model, priorModel);
        listener = new FactoredSequenceListener(listener, priorListener);

        SequenceGibbsSampler sampler = new SequenceGibbsSampler(0, 0, listener);
        int[] sequence = new int[cliqueTree.length()];

        if (flags.initViterbi) {
            TestSequenceModel testSequenceModel = new TestSequenceModel(cliqueTree);
            ExactBestSequenceFinder tagInference = new ExactBestSequenceFinder();
            int[] bestSequence = tagInference.bestSequence(testSequenceModel);
            System.arraycopy(bestSequence, windowSize - 1, sequence, 0, sequence.length);
        } else {
            int[] initialSequence = SequenceGibbsSampler.getRandomSequence(model);
            System.arraycopy(initialSequence, 0, sequence, 0, sequence.length);
        }

        sampler.verbose = 0;

        if (flags.annealingType.equalsIgnoreCase("linear")) {
            sequence = sampler.findBestUsingAnnealing(model, CoolingSchedule.getLinearSchedule(1.0, flags.numSamples),
                    sequence);
        } else if (flags.annealingType.equalsIgnoreCase("exp") || flags.annealingType.equalsIgnoreCase("exponential")) {
            sequence = sampler.findBestUsingAnnealing(model, CoolingSchedule.getExponentialSchedule(1.0, flags.annealingRate,
                    flags.numSamples), sequence);
        } else {
            throw new RuntimeException("No annealing type specified");
        }

        // System.err.println(ArrayMath.toString(sequence));

        if (flags.useReverse) {
            Collections.reverse(document);
        }

        for (int j = 0, dsize = newDocument.size(); j < dsize; j++) {
            IN wi = document.get(j);
            if (wi == null) throw new RuntimeException("");
            if (classIndex == null) throw new RuntimeException("");
            wi.set(CoreAnnotations.AnswerAnnotation.class, classIndex.get(sequence[j]));
        }

        if (flags.useReverse) {
            Collections.reverse(document);
        }

        return document;
    }

    /**
     * Takes a {@link List} of {@link CoreLabel}s and prints the likelihood of
     * each possible label at each point.
     *
     * @param document
     *            A {@link List} of {@link CoreLabel}s.
     */
    @Override
    public synchronized void printProbsDocument(List<IN> document) {

        Triple<int[][][], int[], double[][][]> p = documentToDataAndLabels(document);

        CRFCliqueTree<String> cliqueTree = getCliqueTree(p);

        // for (int i = 0; i < factorTables.length; i++) {
        for (int i = 0; i < cliqueTree.length(); i++) {
            IN wi = document.get(i);
            System.out.print(wi.get(CoreAnnotations.TextAnnotation.class) + '\t');
            for (Iterator<String> iter = classIndex.iterator(); iter.hasNext();) {
                String label = iter.next();
                int index = classIndex.indexOf(label);
                // double prob = Math.pow(Math.E, factorTables[i].logProbEnd(index));
                double prob = cliqueTree.prob(i, index);
                System.out.print(label + '=' + prob);
                if (iter.hasNext()) {
                    System.out.print("\t");
                } else {
                    System.out.print("\n");
                }
            }
        }
    }

    /**
     * Takes the file, reads it in, and prints out the likelihood of each possible
     * label at each point. This gives a simple way to examine the probability
     * distributions of the CRF. See <code>getCliqueTrees()</code> for more.
     *
     * @param filename
     *          The path to the specified file
     */
    public synchronized void printFirstOrderProbs(String filename, DocumentReaderAndWriter<IN> readerAndWriter) {
        // only for the OCR data does this matter
        flags.ocrTrain = false;

        ObjectBank<List<IN>> docs = makeObjectBankFromFile(filename, readerAndWriter);
        printFirstOrderProbsDocuments(docs);
    }

    /**
     * Takes the file, reads it in, and prints out the factor table at each position.
     *
     * @param filename
     *          The path to the specified file
     */
    public synchronized void printFactorTable(String filename, DocumentReaderAndWriter<IN> readerAndWriter) {
        // only for the OCR data does this matter
        flags.ocrTrain = false;

        ObjectBank<List<IN>> docs = makeObjectBankFromFile(filename, readerAndWriter);
        printFactorTableDocuments(docs);
    }

    /**
     * Takes a {@link List} of documents and prints the likelihood of each
     * possible label at each point.
     *
     * @param documents
     *          A {@link List} of {@link List} of INs.
     */
    public synchronized void printFirstOrderProbsDocuments(ObjectBank<List<IN>> documents) {
        for (List<IN> doc : documents) {
            printFirstOrderProbsDocument(doc);
            System.out.println();
        }
    }

    /**
     * Takes a {@link List} of documents and prints the factor table
     * at each point.
     *
     * @param documents
     *          A {@link List} of {@link List} of INs.
     */
    public synchronized void printFactorTableDocuments(ObjectBank<List<IN>> documents) {
        for (List<IN> doc : documents) {
            printFactorTableDocument(doc);
            System.out.println();
        }
    }


    /**
     * Takes a {@link List} of something that extends {@link CoreMap} and prints
     * the factor table at each point.
     *
     * @param document
     *          A {@link List} of something that extends {@link CoreMap}.
     */
    public synchronized void printFactorTableDocument(List<IN> document) {

        CRFCliqueTree<String> cliqueTree = getCliqueTree(document);
        FactorTable[] factorTables = cliqueTree.getFactorTables();

        StringBuilder sb = new StringBuilder();
        for (int i=0; i < factorTables.length; i++) {
            IN wi = document.get(i);
            sb.append(wi.get(CoreAnnotations.TextAnnotation.class));
            sb.append("\t");
            FactorTable table = factorTables[i];
            double totalMass = table.totalMass();
            for (int j = 0; j < table.size(); j++) {
                int[] arr = table.toArray(j);
                sb.append(classIndex.get(arr[0]));
                sb.append(":");
                sb.append(classIndex.get(arr[1]));
                sb.append(":");
                sb.append(table.getValue(j) - totalMass);
                sb.append(" ");
            }
            sb.append("\n");
        }
        System.out.print(sb.toString());
    }

    /**
     * Want to make arbitrary probability queries? Then this is the method for
     * you. Given the filename, it reads it in and breaks it into documents, and
     * then makes a CRFCliqueTree for each document. you can then ask the clique
     * tree for marginals and conditional probabilities of almost anything you
     * want.
     */
    public synchronized List<CRFCliqueTree<String>> getCliqueTrees(String filename, DocumentReaderAndWriter<IN> readerAndWriter) {
        // only for the OCR data does this matter
        flags.ocrTrain = false;

        List<CRFCliqueTree<String>> cts = new ArrayList<CRFCliqueTree<String>>();
        ObjectBank<List<IN>> docs = makeObjectBankFromFile(filename, readerAndWriter);
        for (List<IN> doc : docs) {
            cts.add(getCliqueTree(doc));
        }

        return cts;
    }

    public synchronized CRFCliqueTree<String> getCliqueTree(Triple<int[][][], int[], double[][][]> p) {
        int[][][] data = p.first();
        double[][][] featureVal = p.third();

        return CRFCliqueTree.getCalibratedCliqueTree(data, labelIndices, classIndex.size(), classIndex,
                flags.backgroundSymbol, getCliquePotentialFunction(), featureVal);
    }

    public synchronized CRFCliqueTree<String> getCliqueTree(List<IN> document) {
        Triple<int[][][], int[], double[][][]> p = documentToDataAndLabels(document);
        return getCliqueTree(p);
    }


    private CliquePotentialFunction getCliquePotentialFunction() {
        if (cliquePotentialFunction == null) {
            if (flags.nonLinearCRF) {
                if (flags.secondOrderNonLinear)
                    cliquePotentialFunction = new NonLinearSecondOrderCliquePotentialFunction(inputLayerWeights4Edge, outputLayerWeights4Edge, inputLayerWeights, outputLayerWeights, flags);
                else
                    cliquePotentialFunction = new NonLinearCliquePotentialFunction(linearWeights, inputLayerWeights, outputLayerWeights, flags);
            } else {
                cliquePotentialFunction = new LinearCliquePotentialFunction(weights);
            }
        }
        return cliquePotentialFunction;
    }


    /**
     * Takes a {@link List} of something that extends {@link CoreMap} and prints
     * the likelihood of each possible label at each point.
     *
     * @param document
     *          A {@link List} of something that extends {@link CoreMap}.
     */
    public synchronized void printFirstOrderProbsDocument(List<IN> document) {

        CRFCliqueTree<String> cliqueTree = getCliqueTree(document);

        // for (int i = 0; i < factorTables.length; i++) {
        for (int i = 0; i < cliqueTree.length(); i++) {
            IN wi = document.get(i);
            System.out.print(wi.get(CoreAnnotations.TextAnnotation.class) + '\t');
            for (Iterator<String> iter = classIndex.iterator(); iter.hasNext();) {
                String label = iter.next();
                int index = classIndex.indexOf(label);
                if (i == 0) {
                    // double prob = Math.pow(Math.E, factorTables[i].logProbEnd(index));
                    double prob = cliqueTree.prob(i, index);
                    System.out.print(label + '=' + prob);
                    if (iter.hasNext()) {
                        System.out.print("\t");
                    } else {
                        System.out.print("\n");
                    }
                } else {
                    for (Iterator<String> iter1 = classIndex.iterator(); iter1.hasNext();) {
                        String label1 = iter1.next();
                        int index1 = classIndex.indexOf(label1);
                        // double prob = Math.pow(Math.E, factorTables[i].logProbEnd(new
                        // int[]{index1, index}));
                        double prob = cliqueTree.prob(i, new int[] { index1, index });
                        System.out.print(label1 + '_' + label + '=' + prob);
                        if (iter.hasNext() || iter1.hasNext()) {
                            System.out.print("\t");
                        } else {
                            System.out.print("\n");
                        }
                    }
                }
            }
        }
    }

    /**
     * Train a classifier from documents.
     *
     * @param docs
     *            An objectbank representation of documents.
     */
    @Override
    public synchronized void train(Collection<List<IN>> docs, DocumentReaderAndWriter<IN> readerAndWriter) {

        Timing timer = new Timing();
        timer.start();

        adjustShapeAnnotation(docs); // added FL

        if (flags.numOfSlices > 0) {
            System.out.println("Taking " + flags.numOfSlices + " out of " + flags.totalDataSlice + " slices of data for training");
            List<List<IN>> docsToShuffle = new ArrayList<List<IN>>();
            for (List<IN> doc : docs) {
                docsToShuffle.add(doc);
            }
            Collections.shuffle(docsToShuffle, random);
            int cutOff = (int)(docsToShuffle.size() / (flags.totalDataSlice + 0.0) * flags.numOfSlices);
            docs = docsToShuffle.subList(0, cutOff);
        }

        makeAnswerArraysAndTagIndex(docs);

        long elapsedMs = timer.stop();
        System.out.println("Time to convert docs to feature indices: " + Timing.toSecondsString(elapsedMs) + " seconds");
        if (flags.exportFeatures != null) {
            timer.start();
            CRFFeatureExporter<IN> featureExporter = new CRFFeatureExporter<IN>(this);
            featureExporter.printFeatures(flags.exportFeatures, docs);
            elapsedMs = timer.stop();
            System.out.println("Time to export features: " + Timing.toSecondsString(elapsedMs) + " seconds");
        }

        for (int i = 0; i <= flags.numTimesPruneFeatures; i++) {
            timer.start();
            Triple<int[][][][], int[][], double[][][][]> dataAndLabelsAndFeatureVals = documentsToDataAndLabels(docs);
            elapsedMs = timer.stop();
            System.out.println("Time to convert docs to data/labels: " + Timing.toSecondsString(elapsedMs) + " seconds");

            Evaluator[] evaluators = null;
            if (flags.evaluateIters > 0 || flags.terminateOnEvalImprovement) {
                List<Evaluator> evaluatorList = new ArrayList<Evaluator>();
                if (flags.useMemoryEvaluator)
                    evaluatorList.add(new MemoryEvaluator());
                if (flags.evaluateTrain) {
                    CRFClassifierEvaluator<IN> crfEvaluator = new CRFClassifierEvaluator<IN>("Train set", this);
                    List<Triple<int[][][], int[], double[][][]>> trainDataAndLabels = new ArrayList<Triple<int[][][], int[], double[][][]>>();
                    int[][][][] data = dataAndLabelsAndFeatureVals.first();
                    int[][] labels = dataAndLabelsAndFeatureVals.second();
                    double[][][][] featureVal = dataAndLabelsAndFeatureVals.third();
                    for (int j = 0; j < data.length; j++) {
                        Triple<int[][][], int[], double[][][]> p = new Triple<int[][][], int[], double[][][]>(data[j], labels[j], featureVal[j]);
                        trainDataAndLabels.add(p);
                    }
                    crfEvaluator.setTestData(docs, trainDataAndLabels);
                    if (flags.evalCmd.length() > 0)
                        crfEvaluator.setEvalCmd(flags.evalCmd);
                    evaluatorList.add(crfEvaluator);
                }
                if (flags.testFile != null) {
                    CRFClassifierEvaluator<IN> crfEvaluator = new CRFClassifierEvaluator<IN>("Test set (" + flags.testFile + ")",
                            this);
                    ObjectBank<List<IN>> testObjBank = makeObjectBankFromFile(flags.testFile, readerAndWriter);
                    List<List<IN>> testDocs = new ArrayList<List<IN>>();
                    for (List<IN> doc : testObjBank) {
                        testDocs.add(doc);
                    }
                    List<Triple<int[][][], int[], double[][][]>> testDataAndLabels = documentsToDataAndLabelsList(testDocs);
                    crfEvaluator.setTestData(testDocs, testDataAndLabels);
                    if (flags.evalCmd.length() > 0)
                        crfEvaluator.setEvalCmd(flags.evalCmd);
                    evaluatorList.add(crfEvaluator);
                }
                if (flags.testFiles != null) {
                    String[] testFiles = flags.testFiles.split(",");
                    for (String testFile : testFiles) {
                        CRFClassifierEvaluator<IN> crfEvaluator = new CRFClassifierEvaluator<IN>("Test set ("
                                + testFile + ")", this);
                        ObjectBank<List<IN>> testObjBank = makeObjectBankFromFile(testFile, readerAndWriter);
                        List<Triple<int[][][], int[], double[][][]>> testDataAndLabels = documentsToDataAndLabelsList(testObjBank);
                        crfEvaluator.setTestData(testObjBank, testDataAndLabels);
                        if (flags.evalCmd.length() > 0)
                            crfEvaluator.setEvalCmd(flags.evalCmd);
                        evaluatorList.add(crfEvaluator);
                    }
                }
                evaluators = new Evaluator[evaluatorList.size()];
                evaluatorList.toArray(evaluators);
            }

            if (flags.numTimesPruneFeatures == i) {
                docs = null; // hopefully saves memory
            }
            // save feature index to disk and read in later
            File featIndexFile = null;

            int numFeatures = featureIndex.size();
            if (flags.saveFeatureIndexToDisk) {
                try {
                    System.out.println("Writing feature index to temporary file.");
                    featIndexFile = IOUtils.writeObjectToTempFile(featureIndex, "featIndex" + i + ".tmp");
                    featureIndex = null;
                } catch (IOException e) {
                    throw new RuntimeException("Could not open temporary feature index file for writing.");
                }
            }

            // first index is the number of the document
            // second index is position in the document also the index of the
            // clique/factor table
            // third index is the number of elements in the clique/window thase
            // features are for (starting with last element)
            // fourth index is position of the feature in the array that holds them
            // element in data[i][j][k][m] is the index of the mth feature occurring
            // in position k of the jth clique of the ith document
            int[][][][] data = dataAndLabelsAndFeatureVals.first();
            // first index is the number of the document
            // second index is the position in the document
            // element in labels[i][j] is the index of the correct label (if it
            // exists) at position j in document i
            int[][] labels = dataAndLabelsAndFeatureVals.second();
            double[][][][] featureVals = dataAndLabelsAndFeatureVals.third();

            if (flags.loadProcessedData != null) {
                List<List<CRFDatum<Collection<String>, String>>> processedData = loadProcessedData(flags.loadProcessedData);
                if (processedData != null) {
                    // enlarge the data and labels array
                    int[][][][] allData = new int[data.length + processedData.size()][][][];
                    double[][][][] allFeatureVals = new double[featureVals.length + processedData.size()][][][];
                    int[][] allLabels = new int[labels.length + processedData.size()][];
                    System.arraycopy(data, 0, allData, 0, data.length);
                    System.arraycopy(labels, 0, allLabels, 0, labels.length);
                    System.arraycopy(featureVals, 0, allFeatureVals, 0, featureVals.length);
                    // add to the data and labels array
                    addProcessedData(processedData, allData, allLabels, allFeatureVals, data.length);
                    data = allData;
                    labels = allLabels;
                    featureVals = allFeatureVals;
                }
            }

            if (flags.nonLinearCRF) {
                if (flags.secondOrderNonLinear) {
                    CRFNonLinearSecondOrderLogConditionalObjectiveFunction func = new CRFNonLinearSecondOrderLogConditionalObjectiveFunction(data, labels,
                            windowSize, classIndex, labelIndices, map, flags, nodeFeatureIndicesMap.size(), edgeFeatureIndicesMap.size());
                    cliquePotentialFunctionHelper = func;

                    double[] allWeights = trainWeightsUsingNonLinearCRF(func, evaluators);
                    Quadruple<double[][], double[][], double[][], double[][]> params = func.separateWeights(allWeights);
                    this.inputLayerWeights4Edge = params.first();
                    this.outputLayerWeights4Edge = params.second();
                    this.inputLayerWeights = params.third();
                    this.outputLayerWeights = params.fourth();
                    System.out.println("Edge Output Layer Weights:");
                    for (int ii = 0; ii < outputLayerWeights4Edge.length; ii++) {
                        System.out.print("[ ");
                        for (int jj = 0; jj < outputLayerWeights4Edge[ii].length; jj++) {
                            System.out.print(outputLayerWeights4Edge[ii][jj] + " ");
                        }
                        System.out.println("]");
                    }
                    System.out.println("Node Output Layer Weights:");
                    for (int ii = 0; ii < outputLayerWeights.length; ii++) {
                        System.out.print("[ ");
                        for (int jj = 0; jj < outputLayerWeights[ii].length; jj++) {
                            System.out.print(outputLayerWeights[ii][jj] + " ");
                        }
                        System.out.println("]");
                    }
                } else {
                    CRFNonLinearLogConditionalObjectiveFunction func = new CRFNonLinearLogConditionalObjectiveFunction(data, labels,
                            windowSize, classIndex, labelIndices, map, flags, nodeFeatureIndicesMap.size(), edgeFeatureIndicesMap.size(), featureVals);
                    if (flags.useAdaGradFOBOS) {
                        func.gradientsOnly = true;
                    }
                    cliquePotentialFunctionHelper = func;

                    double[] allWeights = trainWeightsUsingNonLinearCRF(func, evaluators);
                    Triple<double[][], double[][], double[][]> params = func.separateWeights(allWeights);
                    this.linearWeights = params.first();
                    this.inputLayerWeights = params.second();
                    this.outputLayerWeights = params.third();
                    if (flags.printWeights) {
                        System.out.println("Linear Layer Weights:");
                        for (int ii = 0; ii < linearWeights.length; ii++) {
                            // for (int ii = 0; ii < Math.min(1, linearWeights.length); ii++) {
                            System.out.print("[ ");
                            for (int jj = 0; jj < linearWeights[ii].length; jj++) {
                                System.out.print(linearWeights[ii][jj] + " ");
                            }
                            System.out.println("]");
                        }
                        System.out.println("Input Layer Weights:");
                        for (int ii = 0; ii < inputLayerWeights.length; ii++) {
                            // for (int ii = 0; ii < Math.min(1, inputLayerWeights.length); ii++) {
                            System.out.print("[ ");
                            for (int jj = 0; jj < inputLayerWeights[ii].length; jj++) {
                                System.out.print(inputLayerWeights[ii][jj] + " ");
                            }
                            System.out.println("]");
                        }
                        System.out.println("Output Layer Weights:");
                        for (int ii = 0; ii < outputLayerWeights.length; ii++) {
                            System.out.print("[ ");
                            for (int jj = 0; jj < outputLayerWeights[ii].length; jj++) {
                                System.out.print(outputLayerWeights[ii][jj] + " ");
                            }
                            System.out.println("]");
                        }
                    }
                }
            } else {
                double[] oneDimWeights = null;
                if (flags.useFloat) {
                    oneDimWeights = trainWeightsUsingFloatCRF(data, labels, i);
                } else if (flags.numLopExpert > 1) {
                    oneDimWeights = trainWeightsUsingLopCRF(numFeatures, data, labels, evaluators, i);
                } else {
                    oneDimWeights = trainWeightsUsingDoubleCRF(data, labels, evaluators, i, featureVals);
                }
                this.weights = CRFLogConditionalObjectiveFunction.to2D(oneDimWeights, labelIndices, map);
            }

            // save feature index to disk and read in later
            if (flags.saveFeatureIndexToDisk) {
                try {
                    System.out.println("Reading temporary feature index file.");
                    featureIndex = (Index<String>) IOUtils.readObjectFromFile(featIndexFile);
                } catch (Exception e) {
                    throw new RuntimeException("Could not open temporary feature index file for reading.");
                }
            }

            if (i != flags.numTimesPruneFeatures) {
                dropFeaturesBelowThreshold(flags.featureDiffThresh);
                System.out.println("Removing features with weight below " + flags.featureDiffThresh + " and retraining...");
            }
        }
    }



    /**
     * Adds the List of Lists of CRFDatums to the data and labels arrays, treating
     * each datum as if it were its own document. Adds context labels in addition
     * to the target label for each datum, meaning that for a particular document,
     * the number of labels will be windowSize-1 greater than the number of
     * datums.
     *
     * @param processedData
     *          a List of Lists of CRFDatums
     */
    protected void addProcessedData(List<List<CRFDatum<Collection<String>, String>>> processedData, int[][][][] data,
                                    int[][] labels, double[][][][] featureVals, int offset) {
        for (int i = 0, pdSize = processedData.size(); i < pdSize; i++) {
            int dataIndex = i + offset;
            List<CRFDatum<Collection<String>, String>> document = processedData.get(i);
            int dsize = document.size();
            labels[dataIndex] = new int[dsize];
            data[dataIndex] = new int[dsize][][];
            if (featureVals != null)
                featureVals[dataIndex] = new double[dsize][][];
            for (int j = 0; j < dsize; j++) {
                CRFDatum<Collection<String>, String> crfDatum = document.get(j);
                // add label, they are offset by extra context
                labels[dataIndex][j] = classIndex.indexOf(crfDatum.label());
                // add featureVals
                List<double[]> featureValList = null;
                if (featureVals != null)
                    featureValList = crfDatum.asFeatureVals();
                // add features
                List<Collection<String>> cliques = crfDatum.asFeatures();
                int csize = cliques.size();
                data[dataIndex][j] = new int[csize][];
                if (featureVals != null)
                    featureVals[dataIndex][j] = new double[csize][];
                for (int k = 0; k < csize; k++) {
                    Collection<String> features = cliques.get(k);

                    data[dataIndex][j][k] = new int[features.size()];
                    if (featureVals != null)
                        featureVals[dataIndex][j][k] = featureValList.get(k);

                    int m = 0;
                    try {
                        for (String feature : features) {
                            // System.out.println("feature " + feature);
                            // if (featureIndex.indexOf(feature)) ;
                            if (featureIndex == null) {
                                System.out.println("Feature is NULL!");
                            }
                            data[dataIndex][j][k][m] = featureIndex.indexOf(feature);
                            m++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.printf("[index=%d, j=%d, k=%d, m=%d]%n", dataIndex, j, k, m);
                        System.out.println("data.length                    " + data.length);
                        System.out.println("data[dataIndex].length         " + data[dataIndex].length);
                        System.out.println("data[dataIndex][j].length      " + data[dataIndex][j].length);
                        System.out.println("data[dataIndex][j][k].length   " + data[dataIndex][j].length);
                        System.out.println("data[dataIndex][j][k][m]       " + data[dataIndex][j][k][m]);
                        return;
                    }
                }
            }
        }
    }



    /**
     * Convert an ObjectBank to corresponding collection of data features and
     * labels. This version is used at test time.
     *
     * @return A List of pairs, one for each document, where the first element is
     *         an int[][][] representing the data and the second element is an
     *         int[] representing the labels.
     */
    public synchronized List<Triple<int[][][], int[], double[][][]>> documentsToDataAndLabelsList(Collection<List<IN>> documents) {
        int numDatums = 0;

        List<Triple<int[][][], int[], double[][][]>> docList = new ArrayList<Triple<int[][][], int[], double[][][]>>();
        for (List<IN> doc : documents) {
            Triple<int[][][], int[], double[][][]> docTriple = documentToDataAndLabels(doc);
            docList.add(docTriple);
            numDatums += doc.size();
        }

        System.out.println("numClasses: " + classIndex.size() + ' ' + classIndex);
        System.out.println("numDocuments: " + docList.size());
        System.out.println("numDatums: " + numDatums);
        System.out.println("numFeatures: " + featureIndex.size());
        return docList;
    }



    protected double[] trainWeightsUsingNonLinearCRF(AbstractCachingDiffFunction func, Evaluator[] evaluators) {
        Minimizer minimizer = getMinimizer(0, evaluators);

        double[] initialWeights;
        if (flags.initialWeights == null) {
            initialWeights = func.initial();
        } else {
            try {
                System.out.println("Reading initial weights from file " + flags.initialWeights);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(
                        flags.initialWeights))));
                initialWeights = ConvertByteArray.readDoubleArr(dis);
            } catch (IOException e) {
                throw new RuntimeException("Could not read from double initial weight file " + flags.initialWeights);
            }
        }
        System.out.println("numWeights: " + initialWeights.length);

        if (flags.testObjFunction) {
            StochasticDiffFunctionTester tester = new StochasticDiffFunctionTester(func);
            if (tester.testSumOfBatches(initialWeights, 1e-4)) {
                System.out.println("Testing complete... exiting");
                System.exit(1);
            } else {
                System.err.println("Testing failed....exiting");
                System.exit(1);
            }

        }
        //check gradient
        if (flags.checkGradient) {
            if (func.gradientCheck()) {
                System.err.println("gradient check passed");
            } else {
                throw new RuntimeException("gradient check failed");
            }
        }
        return minimizer.minimize(func, flags.tolerance, initialWeights);
    }



    protected double[] trainWeightsUsingDoubleCRF(int[][][][] data, int[][] labels, Evaluator[] evaluators, int pruneFeatureItr, double[][][][] featureVals) {

        CRFLogConditionalObjectiveFunction func = new CRFLogConditionalObjectiveFunction(data, labels,
                windowSize, classIndex, labelIndices, map, flags.priorType, flags.backgroundSymbol, flags.sigma, featureVals);
        cliquePotentialFunctionHelper = func;

        // create feature grouping
        Map<String, Set<Integer>> featureSets = null;
        if (flags.groupByOutputClass) {
            featureSets = new ConcurrentHashMap<String, Set<Integer>>();
            if (flags.groupByFeatureTemplate) {
                int pIndex = 0;
                for (int fIndex = 0; fIndex < map.length; fIndex++) {
                    int cliqueType = map[fIndex];
                    int numCliqueTypeOutputClass = labelIndices.get(map[fIndex]).size();
                    for (int cliqueOutClass = 0; cliqueOutClass < numCliqueTypeOutputClass; cliqueOutClass++) {
                        String name = "c:"+cliqueType+"-o:"+cliqueOutClass+"-g:"+featureIndexToTemplateIndex.get(fIndex);
                        if (featureSets.containsKey(name)) {
                            featureSets.get(name).add(pIndex);
                        } else {
                            Set<Integer> newSet = new HashSet<Integer>();
                            newSet.add(pIndex);
                            featureSets.put(name, newSet);
                        }
                        pIndex++;
                    }
                }
            } else {
                int pIndex = 0;
                for (int cliqueType : map) {
                    int numCliqueTypeOutputClass = labelIndices.get(cliqueType).size();
                    for (int cliqueOutClass = 0; cliqueOutClass < numCliqueTypeOutputClass; cliqueOutClass++) {
                        String name = "c:" + cliqueType + "-o:" + cliqueOutClass;
                        if (featureSets.containsKey(name)) {
                            featureSets.get(name).add(pIndex);
                        } else {
                            Set<Integer> newSet = new HashSet<Integer>();
                            newSet.add(pIndex);
                            featureSets.put(name, newSet);
                        }
                        pIndex++;
                    }
                }
            }
        } else if (flags.groupByFeatureTemplate) {
            featureSets = new ConcurrentHashMap<String, Set<Integer>>();
            int pIndex = 0;
            for (int fIndex = 0; fIndex < map.length; fIndex++) {
                int cliqueType = map[fIndex];
                int numCliqueTypeOutputClass = labelIndices.get(map[fIndex]).size();
                for (int cliqueOutClass = 0; cliqueOutClass < numCliqueTypeOutputClass; cliqueOutClass++) {
                    String name = "c:"+cliqueType+"-g:"+featureIndexToTemplateIndex.get(fIndex);
                    if (featureSets.containsKey(name)) {
                        featureSets.get(name).add(pIndex);
                    } else {
                        Set<Integer> newSet = new HashSet<Integer>();
                        newSet.add(pIndex);
                        featureSets.put(name, newSet);
                    }
                    pIndex++;
                }
            }
        }
        if (featureSets != null) {
            int[][] fg = new int[featureSets.size()][];
            System.out.println("After feature grouping, total of "+fg.length+" groups");
            int count = 0;
            for (Set<Integer> aSet: featureSets.values()) {
                fg[count] = new int[aSet.size()];
                int i = 0;
                for (Integer val : aSet)
                    fg[count][i++] = val;
                count++;
            }
            func.setFeatureGrouping(fg);
        }

        Minimizer minimizer = getMinimizer(pruneFeatureItr, evaluators);

        double[] initialWeights;
        if (flags.initialWeights == null) {
            initialWeights = func.initial();
        } else {
            try {
                System.out.println("Reading initial weights from file " + flags.initialWeights);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(
                        flags.initialWeights))));
                initialWeights = ConvertByteArray.readDoubleArr(dis);
            } catch (IOException e) {
                throw new RuntimeException("Could not read from double initial weight file " + flags.initialWeights);
            }
        }
        System.out.println("numWeights: " + initialWeights.length);

        if (flags.testObjFunction) {
            StochasticDiffFunctionTester tester = new StochasticDiffFunctionTester(func);
            if (tester.testSumOfBatches(initialWeights, 1e-4)) {
                System.out.println("Testing complete... exiting");
                System.exit(1);
            } else {
                System.err.println("Testing failed....exiting");
                System.exit(1);
            }

        }
        //check gradient
        if (flags.checkGradient) {
            if (func.gradientCheck()) {
                System.out.println("gradient check passed");
            } else {
                throw new RuntimeException("gradient check failed");
            }
        }
        return minimizer.minimize(func, flags.tolerance, initialWeights);
    }


    protected double[] trainWeightsUsingLopCRF(int numFeatures, int[][][][] data, int[][] labels, Evaluator[] evaluators, int pruneFeatureItr) {
        int numLopExpert = flags.numLopExpert;
        double[][] lopExpertWeights = new double[numLopExpert][];

        getFeatureBoundaryIndices(numFeatures, numLopExpert);

        if (flags.initialLopWeights != null) {
            try {
                System.out.println("Reading initial LOP weights from file " + flags.initialLopWeights + " ...");
                BufferedReader br = IOUtils.readerFromString(flags.initialLopWeights);
                List<double[]> listOfWeights = new ArrayList<double[]>(numLopExpert);
                for (String line; (line = br.readLine()) != null; ) {
                    line = line.trim();
                    String[] parts = line.split("\t");
                    double[] wArr = new double[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        wArr[i] = Double.parseDouble(parts[i]);
                    }
                    listOfWeights.add(wArr);
                }
                assert(listOfWeights.size() == numLopExpert);
                System.out.println("Done!");
                for (int i = 0; i < numLopExpert; i++)
                    lopExpertWeights[i] = listOfWeights.get(i);
                // DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(
                //     flags.initialLopWeights))));
                // initialScales = Convert.readDoubleArr(dis);
            } catch (IOException e) {
                throw new RuntimeException("Could not read from double initial LOP weights file " + flags.initialLopWeights);
            }
        } else {
            for (int lopIter = 0; lopIter < numLopExpert; lopIter++) {
                int[][][][] partialData = createPartialDataForLOP(lopIter, data);
                if (flags.randomLopWeights) {
                    lopExpertWeights[lopIter] = initWeightsUsingDoubleCRF(partialData, labels, evaluators, pruneFeatureItr);
                } else {
                    lopExpertWeights[lopIter] = trainWeightsUsingDoubleCRF(partialData, labels, evaluators, pruneFeatureItr, null);
                }
            }
            if (flags.includeFullCRFInLOP) {
                double[][] newLopExpertWeights = new double[numLopExpert+1][];
                System.arraycopy(lopExpertWeights, 0, newLopExpertWeights, 0, lopExpertWeights.length);
                if (flags.randomLopWeights) {
                    newLopExpertWeights[numLopExpert] = initWeightsUsingDoubleCRF(data, labels, evaluators, pruneFeatureItr);
                } else {
                    newLopExpertWeights[numLopExpert] = trainWeightsUsingDoubleCRF(data, labels, evaluators, pruneFeatureItr, null);
                }

                Set<Integer> newSet = Generics.newHashSet(numFeatures);
                List<Integer> newList = new ArrayList<Integer>(numFeatures);
                for (int fIndex = 0; fIndex < numFeatures; fIndex++) {
                    newSet.add(fIndex);
                    newList.add(fIndex);
                }
                featureIndicesSetArray.add(newSet);
                featureIndicesListArray.add(newList);

                numLopExpert += 1;
                lopExpertWeights = newLopExpertWeights;
            }
        }


        // Dumb scales
        // double[] lopScales = new double[numLopExpert];
        // Arrays.fill(lopScales, 1.0);
        CRFLogConditionalObjectiveFunctionForLOP func = new CRFLogConditionalObjectiveFunctionForLOP(data, labels, lopExpertWeights,
                windowSize, classIndex, labelIndices, map, flags.backgroundSymbol, numLopExpert, featureIndicesSetArray, featureIndicesListArray,
                flags.backpropLopTraining);
        cliquePotentialFunctionHelper = func;

        Minimizer minimizer = getMinimizer(0, evaluators);

        double[] initialScales;
        //TODO(mengqiu) clean this part up when backpropLogTraining == true
        if (flags.initialLopScales == null) {
            initialScales = func.initial();
        } else {
            try {
                System.out.println("Reading initial LOP scales from file " + flags.initialLopScales);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(
                        flags.initialLopScales))));
                initialScales = ConvertByteArray.readDoubleArr(dis);
            } catch (IOException e) {
                throw new RuntimeException("Could not read from double initial LOP scales file " + flags.initialLopScales);
            }
        }

        double[] learnedParams = minimizer.minimize(func, flags.tolerance, initialScales);
        double[] rawScales = func.separateLopScales(learnedParams);
        double[] lopScales = ArrayMath.softmax(rawScales);
        System.out.println("After SoftMax Transformation, learned scales are:");
        for (int lopIter = 0; lopIter < numLopExpert; lopIter++) {
            System.out.println("lopScales[" + lopIter + "] = " + lopScales[lopIter]);
        }
        double[][] learnedLopExpertWeights = lopExpertWeights;
        if (flags.backpropLopTraining) {
            learnedLopExpertWeights = func.separateLopExpertWeights(learnedParams);
        }
        return CRFLogConditionalObjectiveFunctionForLOP.combineAndScaleLopWeights(numLopExpert, learnedLopExpertWeights, lopScales);
    }



    protected int[][][][] createPartialDataForLOP(int lopIter, int[][][][] data) {
        int[] oldFeatures = null;
        int oldFeatureIndex = -1;
        ArrayList<Integer> newFeatureList = new ArrayList<Integer>(1000);
        Set<Integer> featureIndicesSet = featureIndicesSetArray.get(lopIter);

        int[][][][] newData = new int[data.length][][][];
        for (int i = 0; i < data.length; i++) {
            newData[i] = new int[data[i].length][][];
            for (int j = 0; j < data[i].length; j++) {
                newData[i][j] = new int[data[i][j].length][];
                for (int k = 0; k < data[i][j].length; k++) {
                    oldFeatures = data[i][j][k];
                    newFeatureList.clear();
                    for (int l = 0; l < oldFeatures.length; l++) {
                        oldFeatureIndex = oldFeatures[l];
                        if (featureIndicesSet.contains(oldFeatureIndex)) {
                            newFeatureList.add(oldFeatureIndex);
                        }
                    }
                    newData[i][j][k] = new int[newFeatureList.size()];
                    for (int l = 0; l < newFeatureList.size(); ++l) {
                        newData[i][j][k][l] = newFeatureList.get(l);
                    }
                }
            }
        }

        return newData;
    }


    protected double[] initWeightsUsingDoubleCRF(int[][][][] data, int[][] labels, Evaluator[] evaluators, int pruneFeatureItr) {
        CRFLogConditionalObjectiveFunction func = new CRFLogConditionalObjectiveFunction(data, labels, windowSize, classIndex,
                labelIndices, map, flags.priorType, flags.backgroundSymbol, flags.sigma, null);
        return func.initial();
    }

    protected void getFeatureBoundaryIndices(int numFeatures, int numLopExpert) {
        // first find begin/end feature index for each expert
        int interval = numFeatures / numLopExpert;
        featureIndicesSetArray = new ArrayList<Set<Integer>>(numLopExpert);
        featureIndicesListArray =  new ArrayList<List<Integer>>(numLopExpert);
        for (int i = 0; i < numLopExpert; i++) {
            featureIndicesSetArray.add(Generics.<Integer>newHashSet(interval));
            featureIndicesListArray.add(Generics.<Integer>newArrayList(interval));
        }
        if (flags.randomLopFeatureSplit) {
            for (int fIndex = 0; fIndex < numFeatures; fIndex++) {
                int lopIter = random.nextInt(numLopExpert);
                featureIndicesSetArray.get(lopIter).add(fIndex);
                featureIndicesListArray.get(lopIter).add(fIndex);
            }
        } else {
            for (int lopIter = 0; lopIter < numLopExpert; lopIter++) {
                int beginIndex = lopIter * interval;
                int endIndex = (lopIter+1) * interval;
                if (lopIter == numLopExpert - 1) {
                    endIndex = numFeatures;
                }
                for (int fIndex = beginIndex; fIndex < endIndex; fIndex++) {
                    featureIndicesSetArray.get(lopIter).add(fIndex);
                    featureIndicesListArray.get(lopIter).add(fIndex);
                }
            }
        }
        for (int lopIter = 0; lopIter < numLopExpert; lopIter++) {
            Collections.sort(featureIndicesListArray.get(lopIter));
        }
    }


    protected double[] trainWeightsUsingFloatCRF(int[][][][] data, int[][] labels, int pruneFeatureItr) {
        CRFLogConditionalObjectiveFloatFunction func = new CRFLogConditionalObjectiveFloatFunction(data, labels,
                featureIndex, windowSize, classIndex, labelIndices, map, flags.backgroundSymbol, flags.sigma);
        cliquePotentialFunctionHelper = func;

        QNMinimizer minimizer;
        if (flags.interimOutputFreq != 0) {
            FloatFunction monitor = new ResultStoringFloatMonitor(flags.interimOutputFreq, flags.serializeTo);
            minimizer = new QNMinimizer(monitor);
        } else {
            minimizer = new QNMinimizer();
        }

        if (pruneFeatureItr == 0) {
            minimizer.setM(flags.QNsize);
        } else {
            minimizer.setM(flags.QNsize2);
        }

        float[] initialWeights;
        if (flags.initialWeights == null) {
            initialWeights = func.initial();
        } else {
            try {
                System.out.println("Reading initial weights from file " + flags.initialWeights);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(
                        flags.initialWeights))));
                initialWeights = ConvertByteArray.readFloatArr(dis);
            } catch (IOException e) {
                throw new RuntimeException("Could not read from float initial weight file " + flags.initialWeights);
            }
        }
        System.out.println("numWeights: " + initialWeights.length);
        float[] weights = minimizer.minimize(func, (float) flags.tolerance, initialWeights);
        return ArrayMath.floatArrayToDoubleArray(weights);
    }



    private void adjustShapeAnnotation(Collection<List<IN>> docs) {
		
		/*	Prior to training, this method
		 * 	reshapes the ShapeAnnotations of words a bit,
		 * 	leading to (hopefully) slightly better results.
		 * 
		 * 	Called from the train() method.
		 */

        for (List<IN> doc : docs) {
            for (IN d : doc) {
                String w = d.get(ShapeAnnotation.class);
                String w2 = "";
				
				/*
				 * Change shape if necessary: - capital X's within the word -
				 * characters other than x, d (digits), k (marking that word
				 * also occurs as lower case), . (occurs in words like 'dr.')
				 */
                if (w.length() > 1) {
                    w2 = w.substring(0, 1);
                    for (int i = 1; i < w.length(); i++) {
                        if (w.charAt(i) == 'X') {
                            w2 += "x";
                        } else if (w.charAt(i) == 'x' || w.charAt(i) == 'd'
                                || w.charAt(i) == 'k' || w.charAt(i) == '.') {
                            w2 += w.substring(i, i + 1);
                        } else {
                            w2 += "x";
                        }
                    }
                }
                if (!w2.equals("") && !w2.equals(w)) {
                    d.set(ShapeAnnotation.class, w2);
                }
            }
        }
    }




    protected Minimizer getMinimizer() {
        return getMinimizer(0, null);
    }

    protected Minimizer getMinimizer(int featurePruneIteration, Evaluator[] evaluators) {
        Minimizer<DiffFunction> minimizer = null;
        if (flags.useQN) {
            int QNmem;
            if (featurePruneIteration == 0) {
                QNmem = flags.QNsize;
            } else {
                QNmem = flags.QNsize2;
            }

            if (flags.interimOutputFreq != 0) {
                Function monitor = new ResultStoringMonitor(flags.interimOutputFreq, flags.serializeTo);
                minimizer = new QNMinimizer(monitor, QNmem, flags.useRobustQN);
            } else {
                minimizer = new QNMinimizer(QNmem, flags.useRobustQN);
            }

            ((QNMinimizer) minimizer).terminateOnEvalImprovement(flags.terminateOnEvalImprovement);
            ((QNMinimizer) minimizer).setTerminateOnEvalImprovementNumOfEpoch(flags.terminateOnEvalImprovementNumOfEpoch);
            ((QNMinimizer) minimizer).suppressTestPrompt(flags.suppressTestDebug);
            if (flags.useOWLQN) {
                ((QNMinimizer) minimizer).useOWLQN(flags.useOWLQN, flags.priorLambda);
            }
        } else if (flags.useInPlaceSGD) {
            StochasticInPlaceMinimizer<DiffFunction> sgdMinimizer =
                    new StochasticInPlaceMinimizer<DiffFunction>(flags.sigma, flags.SGDPasses, flags.tuneSampleSize, flags.stochasticBatchSize);
            if (flags.useSGDtoQN) {
                QNMinimizer qnMinimizer;
                int QNmem;
                if (featurePruneIteration == 0) {
                    QNmem = flags.QNsize;
                } else {
                    QNmem = flags.QNsize2;
                }
                if (flags.interimOutputFreq != 0) {
                    Function monitor = new ResultStoringMonitor(flags.interimOutputFreq, flags.serializeTo);
                    qnMinimizer = new QNMinimizer(monitor, QNmem, flags.useRobustQN);
                } else {
                    qnMinimizer = new QNMinimizer(QNmem, flags.useRobustQN);
                }
                minimizer = new HybridMinimizer(sgdMinimizer, qnMinimizer, flags.SGDPasses);
            } else {
                minimizer = sgdMinimizer;
            }
        } else if (flags.useAdaGradFOBOS) {
            minimizer = new SGDWithAdaGradAndFOBOS(
                    flags.initRate, flags.priorLambda, flags.SGDPasses, flags.tuneSampleSize, flags.stochasticBatchSize,
                    flags.priorType, flags.priorAlpha);
            ((SGDWithAdaGradAndFOBOS) minimizer).terminateOnEvalImprovement(flags.terminateOnEvalImprovement);
            ((SGDWithAdaGradAndFOBOS) minimizer).setTerminateOnEvalImprovementNumOfEpoch(flags.terminateOnEvalImprovementNumOfEpoch);
            ((SGDWithAdaGradAndFOBOS) minimizer).suppressTestPrompt(flags.suppressTestDebug);
        } else if (flags.useSGDtoQN) {
            minimizer = new SGDToQNMinimizer(flags.initialGain, flags.stochasticBatchSize,
                    flags.SGDPasses, flags.QNPasses, flags.SGD2QNhessSamples,
                    flags.QNsize, flags.outputIterationsToFile);
        } else if (flags.useSMD) {
            minimizer = new SMDMinimizer(flags.initialGain, flags.stochasticBatchSize, flags.stochasticMethod,
                    flags.SGDPasses);
        } else if (flags.useSGD) {
            minimizer = new SGDMinimizer(flags.initialGain, flags.stochasticBatchSize);
        } else if (flags.useScaledSGD) {
            minimizer = new ScaledSGDMinimizer(flags.initialGain, flags.stochasticBatchSize, flags.SGDPasses,
                    flags.scaledSGDMethod);
        } else if (flags.l1reg > 0.0) {
            minimizer = ReflectionLoading.loadByReflection("edu.stanford.nlp.optimization.OWLQNMinimizer", flags.l1reg);
        }

        if (minimizer instanceof HasEvaluators) {
            ((HasEvaluators) minimizer).setEvaluators(flags.evaluateIters, evaluators);
        }
        if (minimizer == null) {
            throw new RuntimeException("No minimizer assigned!");
        }

        return minimizer;
    }

    /**
     * Creates a new CRFDatum from the preprocessed allData format, given the
     * document number, position number, and a List of Object labels.
     *
     * @return A new CRFDatum
     */
    protected List<CRFDatum<? extends Collection<String>, ? extends CharSequence>> extractDatumSequence(int[][][] allData, int beginPosition, int endPosition,
                                                                                                        List<IN> labeledWordInfos) {
        List<CRFDatum<? extends Collection<String>, ? extends CharSequence>> result = new ArrayList<CRFDatum<? extends Collection<String>, ? extends CharSequence>>();
        int beginContext = beginPosition - windowSize + 1;
        if (beginContext < 0) {
            beginContext = 0;
        }
        // for the beginning context, add some dummy datums with no features!
        // TODO: is there any better way to do this?
        for (int position = beginContext; position < beginPosition; position++) {
            List<Collection<String>> cliqueFeatures = new ArrayList<Collection<String>>();
            List<double[]> featureVals = new ArrayList<double[]>();
            for (int i = 0; i < windowSize; i++) {
                // create a feature list
                cliqueFeatures.add(Collections.<String>emptyList());
                featureVals.add(null);
            }
            CRFDatum<Collection<String>, String> datum = new CRFDatum<Collection<String>, String>(cliqueFeatures,
                    labeledWordInfos.get(position).get(CoreAnnotations.AnswerAnnotation.class), featureVals);
            result.add(datum);
        }
        // now add the real datums
        for (int position = beginPosition; position <= endPosition; position++) {
            List<Collection<String>> cliqueFeatures = new ArrayList<Collection<String>>();
            List<double[]> featureVals = new ArrayList<double[]>();
            for (int i = 0; i < windowSize; i++) {
                // create a feature list
                Collection<String> features = new ArrayList<String>();
                for (int j = 0; j < allData[position][i].length; j++) {
                    features.add(featureIndex.get(allData[position][i][j]));
                }
                cliqueFeatures.add(features);
                featureVals.add(null);
            }
            CRFDatum<Collection<String>,String> datum = new CRFDatum<Collection<String>,String>(cliqueFeatures,
                    labeledWordInfos.get(position).get(CoreAnnotations.AnswerAnnotation.class), featureVals);
            result.add(datum);
        }
        return result;
    }

    /**
     * Adds the List of Lists of CRFDatums to the data and labels arrays,
     * treating each datum as if it were its own document. Adds context labels
     * in addition to the target label for each datum, meaning that for a
     * particular document, the number of labels will be windowSize-1 greater
     * than the number of datums.
     *
     * @param processedData
     *            a List of Lists of CRFDatums
     * @param data
     * @param labels
     * @param offset
     */
    protected void addProcessedData(List<List<CRFDatum>> processedData,
                                    int[][][][] data, int[][] labels, int offset) {
        for (int i = 0, pdSize = processedData.size(); i < pdSize; i++) {
            int dataIndex = i + offset;
            List<CRFDatum> document = processedData.get(i);
            int dsize = document.size();
            labels[dataIndex] = new int[dsize];
            data[dataIndex] = new int[dsize][][];
            for (int j = 0; j < dsize; j++) {
                CRFDatum crfDatum = document.get(j);
                // add label, they are offset by extra context
                labels[dataIndex][j] = classIndex.indexOf((String) crfDatum
                        .label());
                // add features
                List<Collection<String>> cliques = crfDatum.asFeatures();
                int csize = cliques.size();
                data[dataIndex][j] = new int[csize][];
                for (int k = 0; k < csize; k++) {
                    Collection<String> features = cliques.get(k);

                    // Debug only: Remove
                    // if (j < windowSize) {
                    // System.out.println("addProcessedData: Features Size: " +
                    // features.size());
                    // }

                    data[dataIndex][j][k] = new int[features.size()];

                    int m = 0;
                    try {
                        for (String feature : features) {
                            // System.out.println("feature " + feature);
                            // if (featureIndex.indexOf(feature)) ;
                            if (featureIndex == null) {
                                System.out.println("Feature is NULL!");
                            }
                            data[dataIndex][j][k][m] = featureIndex
                                    .indexOf(feature);
                            m++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.printf("[index=%d, j=%d, k=%d, m=%d]\n",
                                dataIndex, j, k, m);
                        System.out.println("data.length                    "
                                + data.length);
                        System.out.println("data[dataIndex].length         "
                                + data[dataIndex].length);
                        System.out.println("data[dataIndex][j].length      "
                                + data[dataIndex][j].length);
                        System.out.println("data[dataIndex][j][k].length   "
                                + data[dataIndex][j].length);
                        System.out.println("data[dataIndex][j][k][m]       "
                                + data[dataIndex][j][k][m]);
                        return;
                    }
                }
            }
        }
    }

    protected static void saveProcessedData(List datums, String filename) {
        System.out.print("Saving processsed data of size " + datums.size()
                + " to serialized file...");
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(filename));
            oos.writeObject(datums);
        } catch (IOException e) {
            // do nothing
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                }
            }
        }
        System.out.println("done.");
    }

    protected static List loadProcessedData(String filename) {
        System.out.print("Loading processed data from serialized file...");
        ObjectInputStream ois = null;
        List result = Collections.EMPTY_LIST;
        try {
            ois = new ObjectInputStream(new FileInputStream(filename));
            result = (List) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                }
            }
        }
        System.out.println("done. Got " + result.size() + " datums.");
        return result;
    }

    public synchronized void loadTextClassifier(String text, Properties props) throws ClassCastException, IOException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        // System.out.println("DEBUG: in loadTextClassifier");
        System.out.println("Loading Text Classifier from " + text);
        BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(text))));

        String line = br.readLine();
        // first line should be this format:
        // labelIndices.size()=\t%d
        String[] toks = line.split("\\t");
        if (!toks[0].equals("labelIndices.length=")) {
            throw new RuntimeException("format error");
        }
        int size = Integer.parseInt(toks[1]);
        labelIndices = new ArrayList<Index<CRFLabel>>(size);
        for (int labelIndicesIdx = 0; labelIndicesIdx < size; labelIndicesIdx++) {
            line = br.readLine();
            // first line should be this format:
            // labelIndices.length=\t%d
            // labelIndices[0].size()=\t%d
            toks = line.split("\\t");
            if (!(toks[0].startsWith("labelIndices[") && toks[0].endsWith("].size()="))) {
                throw new RuntimeException("format error");
            }
            int labelIndexSize = Integer.parseInt(toks[1]);
            labelIndices.add(new HashIndex<CRFLabel>());
            int count = 0;
            while (count < labelIndexSize) {
                line = br.readLine();
                toks = line.split("\\t");
                int idx = Integer.parseInt(toks[0]);
                if (count != idx) {
                    throw new RuntimeException("format error");
                }

                String[] crflabelstr = toks[1].split(" ");
                int[] crflabel = new int[crflabelstr.length];
                for (int i = 0; i < crflabelstr.length; i++) {
                    crflabel[i] = Integer.parseInt(crflabelstr[i]);
                }
                CRFLabel crfL = new CRFLabel(crflabel);

                labelIndices.get(labelIndicesIdx).add(crfL);
                count++;
            }
        }

        /**************************************/
        System.err.printf("DEBUG: labelIndices.length=\t%d%n", labelIndices.size());
        for (int i = 0; i < labelIndices.size(); i++) {
            System.err.printf("DEBUG: labelIndices[%d].size()=\t%d%n", i, labelIndices.get(i).size());
            for (int j = 0; j < labelIndices.get(i).size(); j++) {
                int[] label = labelIndices.get(i).get(j).getLabel();
                List<Integer> list = new ArrayList<Integer>();
                for (int l : label) {
                    list.add(l);
                }
                System.err.printf("DEBUG: %d\t%s%n", j, StringUtils.join(list, " "));
            }
        }
        /**************************************/

        line = br.readLine();
        toks = line.split("\\t");
        if (!toks[0].equals("classIndex.size()=")) {
            throw new RuntimeException("format error");
        }
        int classIndexSize = Integer.parseInt(toks[1]);
        classIndex = new HashIndex<String>();
        int count = 0;
        while (count < classIndexSize) {
            line = br.readLine();
            toks = line.split("\\t");
            int idx = Integer.parseInt(toks[0]);
            if (count != idx) {
                throw new RuntimeException("format error");
            }
            classIndex.add(toks[1]);
            count++;
        }

        /******************************************/
        System.err.printf("DEBUG: classIndex.size()=\t%d%n", classIndex.size());
        for (int i = 0; i < classIndex.size(); i++) {
            System.err.printf("DEBUG: %d\t%s%n", i, classIndex.get(i));
        }
        /******************************************/

        line = br.readLine();
        toks = line.split("\\t");
        if (!toks[0].equals("featureIndex.size()=")) {
            throw new RuntimeException("format error");
        }
        int featureIndexSize = Integer.parseInt(toks[1]);
        featureIndex = new HashIndex<String>();
        count = 0;
        while (count < featureIndexSize) {
            line = br.readLine();
            toks = line.split("\\t");
            int idx = Integer.parseInt(toks[0]);
            if (count != idx) {
                throw new RuntimeException("format error");
            }
            featureIndex.add(toks[1]);
            count++;
        }

        /***************************************/
        System.err.printf("DEBUG: featureIndex.size()=\t%d%n", featureIndex.size());
        /***************************************/

  /*
    for(int i = 0; i < featureIndex.size(); i++) {
      System.err.printf("DEBUG: %d\t%s%n", i, featureIndex.get(i));
    }
  */
        /***************************************/

        line = br.readLine();
        if (!line.equals("<flags>")) {
            throw new RuntimeException("format error");
        }
        Properties p = new Properties();
        line = br.readLine();

        while (!line.equals("</flags>")) {
            // System.err.println("DEBUG: flags line: "+line);
            String[] keyValue = line.split("=");
            // System.err.printf("DEBUG: p.setProperty(%s,%s)%n", keyValue[0],
            // keyValue[1]);
            p.setProperty(keyValue[0], keyValue[1]);
            line = br.readLine();
        }

        // System.err.println("DEBUG: out from flags");
        flags = new SeqClassifierFlags(p);
        System.err.println("DEBUG: <flags>");
        System.err.print(flags.toString());
        System.err.println("DEBUG: </flags>");

        if (flags.useEmbedding) {
            line = br.readLine();
            toks = line.split("\\t");
            if (!toks[0].equals("embeddings.size()=")) {
                throw new RuntimeException("format error in embeddings");
            }
            int embeddingSize = Integer.parseInt(toks[1]);
            embeddings = Generics.newHashMap(embeddingSize);
            count = 0;
            while (count < embeddingSize) {
                line = br.readLine().trim();
                toks = line.split("\\t");
                String word = toks[0];
                double[] arr = ArrayUtils.toDoubleArray(toks[1].split(" "));
                embeddings.put(word, arr);
                count++;
            }
        }

        if (flags.nonLinearCRF) {
            line = br.readLine();
            toks = line.split("\\t");
            if (!toks[0].equals("nodeFeatureIndicesMap.size()=")) {
                throw new RuntimeException("format error in nodeFeatureIndicesMap");
            }
            int nodeFeatureIndicesMapSize = Integer.parseInt(toks[1]);
            nodeFeatureIndicesMap = new HashIndex<Integer>();
            count = 0;
            while (count < nodeFeatureIndicesMapSize) {
                line = br.readLine();
                toks = line.split("\\t");
                int idx = Integer.parseInt(toks[0]);
                if (count != idx) {
                    throw new RuntimeException("format error");
                }
                nodeFeatureIndicesMap.add(Integer.parseInt(toks[1]));
                count++;
            }

            /***************************************/
            System.err.printf("DEBUG: nodeFeatureIndicesMap.size()=\t%d%n", nodeFeatureIndicesMap.size());
            /***************************************/

            line = br.readLine();

            toks = line.split("\\t");
            if (!toks[0].equals("edgeFeatureIndicesMap.size()=")) {
                throw new RuntimeException("format error");
            }
            int edgeFeatureIndicesMapSize = Integer.parseInt(toks[1]);
            edgeFeatureIndicesMap = new HashIndex<Integer>();
            count = 0;
            while (count < edgeFeatureIndicesMapSize) {
                line = br.readLine();
                toks = line.split("\\t");
                int idx = Integer.parseInt(toks[0]);
                if (count != idx) {
                    throw new RuntimeException("format error");
                }
                edgeFeatureIndicesMap.add(Integer.parseInt(toks[1]));
                count++;
            }

            /***************************************/
            System.err.printf("DEBUG: edgeFeatureIndicesMap.size()=\t%d%n", edgeFeatureIndicesMap.size());
            /***************************************/

            int  weightsLength = -1;
            if (flags.secondOrderNonLinear) {
                line = br.readLine();
                toks = line.split("\\t");
                if (!toks[0].equals("inputLayerWeights4Edge.length=")) {
                    throw new RuntimeException("format error");
                }
                weightsLength = Integer.parseInt(toks[1]);
                inputLayerWeights4Edge = new double[weightsLength][];
                count = 0;
                while (count < weightsLength) {
                    line = br.readLine();

                    toks = line.split("\\t");
                    int weights2Length = Integer.parseInt(toks[0]);
                    inputLayerWeights4Edge[count] = new double[weights2Length];
                    String[] weightsValue = toks[1].split(" ");
                    if (weights2Length != weightsValue.length) {
                        throw new RuntimeException("weights format error");
                    }

                    for (int i2 = 0; i2 < weights2Length; i2++) {
                        inputLayerWeights4Edge[count][i2] = Double.parseDouble(weightsValue[i2]);
                    }
                    count++;
                }
                /***************************************/
                System.err.printf("DEBUG: double[%d][] inputLayerWeights4Edge loaded%n", weightsLength);
                /***************************************/

                line = br.readLine();

                toks = line.split("\\t");
                if (!toks[0].equals("outputLayerWeights4Edge.length=")) {
                    throw new RuntimeException("format error");
                }
                weightsLength = Integer.parseInt(toks[1]);
                outputLayerWeights4Edge = new double[weightsLength][];
                count = 0;
                while (count < weightsLength) {
                    line = br.readLine();

                    toks = line.split("\\t");
                    int weights2Length = Integer.parseInt(toks[0]);
                    outputLayerWeights4Edge[count] = new double[weights2Length];
                    String[] weightsValue = toks[1].split(" ");
                    if (weights2Length != weightsValue.length) {
                        throw new RuntimeException("weights format error");
                    }

                    for (int i2 = 0; i2 < weights2Length; i2++) {
                        outputLayerWeights4Edge[count][i2] = Double.parseDouble(weightsValue[i2]);
                    }
                    count++;
                }
                /***************************************/
                System.err.printf("DEBUG: double[%d][] outputLayerWeights loaded%n", weightsLength);
                /***************************************/

            } else {
                line = br.readLine();

                toks = line.split("\\t");
                if (!toks[0].equals("linearWeights.length=")) {
                    throw new RuntimeException("format error");
                }
                weightsLength = Integer.parseInt(toks[1]);
                linearWeights = new double[weightsLength][];
                count = 0;
                while (count < weightsLength) {
                    line = br.readLine();

                    toks = line.split("\\t");
                    int weights2Length = Integer.parseInt(toks[0]);
                    linearWeights[count] = new double[weights2Length];
                    String[] weightsValue = toks[1].split(" ");
                    if (weights2Length != weightsValue.length) {
                        throw new RuntimeException("weights format error");
                    }

                    for (int i2 = 0; i2 < weights2Length; i2++) {
                        linearWeights[count][i2] = Double.parseDouble(weightsValue[i2]);
                    }
                    count++;
                }
                /***************************************/
                System.err.printf("DEBUG: double[%d][] linearWeights loaded%n", weightsLength);
                /***************************************/
            }

            line = br.readLine();

            toks = line.split("\\t");
            if (!toks[0].equals("inputLayerWeights.length=")) {
                throw new RuntimeException("format error");
            }
            weightsLength = Integer.parseInt(toks[1]);
            inputLayerWeights = new double[weightsLength][];
            count = 0;
            while (count < weightsLength) {
                line = br.readLine();

                toks = line.split("\\t");
                int weights2Length = Integer.parseInt(toks[0]);
                inputLayerWeights[count] = new double[weights2Length];
                String[] weightsValue = toks[1].split(" ");
                if (weights2Length != weightsValue.length) {
                    throw new RuntimeException("weights format error");
                }

                for (int i2 = 0; i2 < weights2Length; i2++) {
                    inputLayerWeights[count][i2] = Double.parseDouble(weightsValue[i2]);
                }
                count++;
            }
            /***************************************/
            System.err.printf("DEBUG: double[%d][] inputLayerWeights loaded%n", weightsLength);
            /***************************************/

            line = br.readLine();

            toks = line.split("\\t");
            if (!toks[0].equals("outputLayerWeights.length=")) {
                throw new RuntimeException("format error");
            }
            weightsLength = Integer.parseInt(toks[1]);
            outputLayerWeights = new double[weightsLength][];
            count = 0;
            while (count < weightsLength) {
                line = br.readLine();

                toks = line.split("\\t");
                int weights2Length = Integer.parseInt(toks[0]);
                outputLayerWeights[count] = new double[weights2Length];
                String[] weightsValue = toks[1].split(" ");
                if (weights2Length != weightsValue.length) {
                    throw new RuntimeException("weights format error");
                }

                for (int i2 = 0; i2 < weights2Length; i2++) {
                    outputLayerWeights[count][i2] = Double.parseDouble(weightsValue[i2]);
                }
                count++;
            }
            /***************************************/
            System.err.printf("DEBUG: double[%d][] outputLayerWeights loaded%n", weightsLength);
            /***************************************/
        }

        // <featureFactory>
        // edu.stanford.nlp.wordseg.Gale2007ChineseSegmenterFeatureFactory
        // </featureFactory>
        line = br.readLine();

        String[] featureFactoryName = line.split(" ");
        if (!featureFactoryName[0].equals("<featureFactory>") || !featureFactoryName[2].equals("</featureFactory>")) {
            throw new RuntimeException("format error");
        }
        featureFactory = (edu.stanford.nlp.sequences.FeatureFactory<IN>) Class.forName(featureFactoryName[1]).newInstance();
        featureFactory.init(flags);

        reinit();

        // <windowSize> 2 </windowSize>
        line = br.readLine();

        String[] windowSizeName = line.split(" ");
        if (!windowSizeName[0].equals("<windowSize>") || !windowSizeName[2].equals("</windowSize>")) {
            throw new RuntimeException("format error");
        }
        windowSize = Integer.parseInt(windowSizeName[1]);

        // weights.length= 2655170
        line = br.readLine();

        toks = line.split("\\t");
        if (!toks[0].equals("weights.length=")) {
            throw new RuntimeException("format error");
        }
        int weightsLength = Integer.parseInt(toks[1]);
        weights = new double[weightsLength][];
        count = 0;
        while (count < weightsLength) {
            line = br.readLine();

            toks = line.split("\\t");
            int weights2Length = Integer.parseInt(toks[0]);
            weights[count] = new double[weights2Length];
            String[] weightsValue = toks[1].split(" ");
            if (weights2Length != weightsValue.length) {
                throw new RuntimeException("weights format error");
            }

            for (int i2 = 0; i2 < weights2Length; i2++) {
                weights[count][i2] = Double.parseDouble(weightsValue[i2]);
            }
            count++;
        }
        System.err.printf("DEBUG: double[%d][] weights loaded%n", weightsLength);
        line = br.readLine();

        if (line != null) {
            throw new RuntimeException("weights format error");
        }
    }

    /**
     * Serialize the model to a human readable format. It's not yet complete. It
     * should now work for Chinese segmenter though. TODO: check things in
     * serializeClassifier and add other necessary serialization back
     *
     * @param serializePath
     *            File to write text format of classifier to.
     */
    public synchronized void serializeTextClassifier(String serializePath) {
        System.out.print("Serializing Text classifier to " + serializePath
                + "...");
        try {
            PrintWriter pw = new PrintWriter(new GZIPOutputStream(
                    new FileOutputStream(serializePath)));

            pw.printf("labelIndices.length=\t%d\n", labelIndices.size());
            for (int i = 0; i < labelIndices.size(); i++) {
                pw.printf("labelIndices[%d].size()=\t%d\n", i, labelIndices.get(i).size());
                for (int j = 0; j < labelIndices.get(i).size(); j++) {
                    int[] label = labelIndices.get(i).get(j).getLabel();
                    List<Integer> list = new ArrayList<Integer>();
                    for (int l : label) {
                        list.add(l);
                    }
                    pw.printf("%d\t%s\n", j, StringUtils.join(list, " "));
                }
            }

            pw.printf("classIndex.size()=\t%d\n", classIndex.size());
            for (int i = 0; i < classIndex.size(); i++) {
                pw.printf("%d\t%s\n", i, classIndex.get(i));
            }
            // pw.printf("</classIndex>\n");

            pw.printf("featureIndex.size()=\t%d\n", featureIndex.size());
            for (int i = 0; i < featureIndex.size(); i++) {
                pw.printf("%d\t%s\n", i, featureIndex.get(i));
            }
            // pw.printf("</featureIndex>\n");

            pw.println("<flags>");
            pw.print(flags.toString());
            pw.println("</flags>");

            pw.printf("<featureFactory> %s </featureFactory>\n", featureFactory
                    .getClass().getName());

            pw.printf("<windowSize> %d </windowSize>\n", windowSize);

            pw.printf("weights.length=\t%d\n", weights.length);
            for (double[] ws : weights) {
                ArrayList<Double> list = new ArrayList<Double>();
                for (double w : ws) {
                    list.add(w);
                }
                pw.printf("%d\t%s\n", ws.length, StringUtils.join(list, " "));
            }

            pw.close();
            System.out.println("done.");

        } catch (Exception e) {
            System.err.println("Failed");
            e.printStackTrace();
            // don't actually exit in case they're testing too
            // System.exit(1);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void serializeClassifier(String serializePath) {
        System.out.print("Serializing classifier to " + serializePath + "...");

        ObjectOutputStream oos = null;
        try {
            oos = IOUtils.writeStreamFromString(serializePath);
            serializeClassifier(oos);
            System.out.println("done.");

        } catch (Exception e) {
            System.err.println("Failed");
            e.printStackTrace();
            // don't actually exit in case they're testing too
            // System.exit(1);
        } finally {
            IOUtils.closeIgnoringExceptions(oos);
        }
    }

    /**
     * Serialize the classifier to the given ObjectOutputStream.
     * <br>
     * TODO: with a little more effort we could just make the
     * AbstractSequenceClassifier family implement Serializable
     */
    public synchronized void serializeClassifier(ObjectOutputStream oos) {
        try {
            oos.writeObject(labelIndices);
            oos.writeObject(classIndex);
            oos.writeObject(featureIndex);
            oos.writeObject(flags);
            if (flags.useEmbedding)
                oos.writeObject(embeddings);
            if (flags.nonLinearCRF) {
                oos.writeObject(nodeFeatureIndicesMap);
                oos.writeObject(edgeFeatureIndicesMap);
                if (flags.secondOrderNonLinear) {
                    oos.writeObject(inputLayerWeights4Edge);
                    oos.writeObject(outputLayerWeights4Edge);
                } else {
                    oos.writeObject(linearWeights);
                }
                oos.writeObject(inputLayerWeights);
                oos.writeObject(outputLayerWeights);
            }
            oos.writeObject(featureFactory);
            oos.writeInt(windowSize);
            oos.writeObject(weights);
            // oos.writeObject(WordShapeClassifier.getKnownLowerCaseWords());

            oos.writeObject(knownLCWords);

            convertSVHMToSVList();
            oos.writeObject(spelvarlist); // toegevoegd FL
            oos.writeObject(gazettes); // toegevoegd FL
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }



    /**
     * Loads a classifier from the specified InputStream. This version works
     * quietly (unless VERBOSE is true). If props is non-null then any
     * properties it specifies override those in the serialized file. However,
     * only some properties are sensible to change (you shouldn't change how
     * features are defined).
     * <p>
     * <i>Note:</i> This method does not close the ObjectInputStream. (But
     * earlier versions of the code used to, so beware....)
     */
    @Override
    @SuppressWarnings( { "unchecked" })
    // can't have right types in deserialization
    public synchronized void loadClassifier(ObjectInputStream ois, Properties props)
            throws ClassCastException, IOException, ClassNotFoundException {
        Object o = ois.readObject();
        // TODO: when we next break serialization, get rid of this fork and only read the List<Index>
        if (o instanceof List) {
            labelIndices = (List<Index<CRFLabel>>) o;
        } else {
            Index<CRFLabel>[] indexArray = (Index<CRFLabel>[]) o;
            labelIndices = new ArrayList<Index<CRFLabel>>(indexArray.length);
            for (int i = 0; i < indexArray.length; ++i) {
                labelIndices.add(indexArray[i]);
            }
        }
        classIndex = (Index<String>) ois.readObject();
        featureIndex = (Index<String>) ois.readObject();
        flags = (SeqClassifierFlags) ois.readObject();
        featureFactory = (edu.stanford.nlp.sequences.FeatureFactory) ois
                .readObject();

        if (props != null) {
            flags.setProperties(props, false);
        }
        reinit();

        windowSize = ois.readInt();
        weights = (double[][]) ois.readObject();

        // WordShapeClassifier.setKnownLowerCaseWords((Set) ois.readObject());
        knownLCWords = (Set<String>) ois.readObject();

        spelvarlist = (List<String>) ois.readObject();

        gazettes = (List<String>) ois.readObject();

        if (VERBOSE) {
            System.out.println("windowSize=" + windowSize);
            System.out.println("flags=\n" + flags);
        }
    }

    /**
     * This is used to load the default supplied classifier stored within the
     * jar file. THIS FUNCTION WILL ONLY WORK IF THE CODE WAS LOADED FROM A JAR
     * FILE WHICH HAS A SERIALIZED CLASSIFIER STORED INSIDE IT.
     */

    public synchronized void loadDefaultClassifier() {
        loadJarClassifier(DEFAULT_CLASSIFIER, null);
    }

    /**
     * Used to get the default supplied classifier inside the jar file. THIS
     * FUNCTION WILL ONLY WORK IF THE CODE WAS LOADED FROM A JAR FILE WHICH HAS
     * A SERIALIZED CLASSIFIER STORED INSIDE IT.
     *
     * @return The default CRFClassifier in the jar file (if there is one)
     */
	/*
	 * public static CRFClassifier getDefaultClassifier() { CRFClassifier crf =
	 * new CRFClassifier(); crf.loadDefaultClassifier(); return crf; }
	 */
    /**
     * Used to load a classifier stored as a resource inside a jar file. THIS
     * FUNCTION WILL ONLY WORK IF THE CODE WAS LOADED FROM A JAR FILE WHICH HAS
     * A SERIALIZED CLASSIFIER STORED INSIDE IT.
     *
     * @param resourceName
     *            Name of clasifier resource inside the jar file.
     * @return A CRFClassifier stored in the jar file
     */
	/*
	 * public static CRFClassifier getJarClassifier(String resourceName,
	 * Properties props) { CRFClassifier crf = new CRFClassifier();
	 * crf.loadJarClassifier(resourceName, props); return crf; }
	 */
    /**
     * Loads a CRF classifier from a filepath, and returns it.
     *
     * @param file
     *            File to load classifier from
     * @return The CRF classifier
     *
     * @throws IOException
     *             If there are problems accessing the input stream
     * @throws ClassCastException
     *             If there are problems interpreting the serialized data
     * @throws ClassNotFoundException
     *             If there are problems interpreting the serialized data
     */

    public static ImpactCRFClassifier getClassifier(File file)
            throws IOException, ClassCastException, ClassNotFoundException {
        ImpactCRFClassifier crf = new ImpactCRFClassifier();
        crf.loadClassifier(file);
        return crf;
    }



    /**
     * Loads a CRF classifier from an InputStream, and returns it. This method
     * does not buffer the InputStream, so you should have buffered it before
     * calling this method.
     *
     * @param in
     *            InputStream to load classifier from
     * @return The CRF classifier
     * @throws IOException
     *
     * @throws IOException
     *             If there are problems accessing the input stream
     * @throws ClassCastException
     *             If there are problems interpreting the serialized data
     * @throws ClassNotFoundException
     *             If there are problems interpreting the serialized data
     */
	/*
	 * public static ImpactCRFClassifier getClassifier(InputStream in) throws
	 * IOException, ClassCastException, ClassNotFoundException {
	 * ImpactCRFClassifier crf = new ImpactCRFClassifier();
	 * crf.loadClassifier(in); return crf; }
	 * 
	 * public static CRFClassifier getClassifierNoExceptions(String loadPath) {
	 * CRFClassifier crf = new CRFClassifier();
	 * crf.loadClassifierNoExceptions(loadPath); return crf; }
	 * 
	 * public static ImpactCRFClassifier getClassifier(String loadPath) throws
	 * IOException, ClassCastException, ClassNotFoundException {
	 * ImpactCRFClassifier crf = new ImpactCRFClassifier();
	 * crf.loadClassifier(loadPath); return crf; }
	 */
    // called from the RunNERT GUI
	/*
	 * public void startTraining(String arg){ startTraining(arg.split("\\s+"));
	 * }
	 */
	/*
	 * public void startTraining(String[] args){ for(int i = 0; i < args.length;
	 * i++){ System.out.println("arg: "+args[i]); } Properties props =
	 * StringUtils.argsToProperties(args); ImpactCRFClassifier crf = new
	 * ImpactCRFClassifier(props); for(Object k: props.keySet()){ String l =
	 * k.toString(); System.out.println("en? "+l); }
	 */




    public static void initTraining(String[] args) throws IOException {
		
		/*	
		 * 	This method is called from the main NERT-class using flag '-t'.
		 * 	It separates the Stanford from the non-Stanford properties:
		 * 	the properties that cannot be read by the Stanford code are taken out. 
		 * 	These include the format of the trainfile(s), the use of the spelvarreduction
		 * 	module and two spelvar-settings.
		 * 	
		 * 	The input format of the trainfile(s) is set here. Unless indicated in the 
		 * 	properties file with flag 'format', the default format is 'bio'.
		 * 
		 * 	Finally, a crf-object is created and the actual initTraining() method
		 * 	is called.
		 */

        String inputFormat = "bio";
        String xmltag = "";
        String netag1 = "";
        String netag2 = "";

        Properties props = StringUtils.argsToProperties(args);
        Properties stanfordProps = new Properties();
	
		/*	Filter out the non-stanford properties before starting the
		 * 	Stanford code
		 */

        ConcurrentHashMap<String, String> svPropsHM = new ConcurrentHashMap<String, String>();
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String val = props.getProperty(key);

            if (key.equalsIgnoreCase("usespelvar")) {
                useSpelVar = Boolean.parseBoolean(val);
            }
            else if (key.equalsIgnoreCase("svphontrans")) {
                svPropsHM.put(key, val);
            }
            else if (key.equalsIgnoreCase("printspelvarpairs")) {
                svPropsHM.put(key, val);
            }
            else if (key.equalsIgnoreCase("minwordlength")) {
                svPropsHM.put(key, val);
            }
            else if (key.equalsIgnoreCase("format")) {
                inputFormat = val.toLowerCase();
                System.out.println("Input format: "+inputFormat);
            }
            else if (key.equalsIgnoreCase("xmltags")) {
                xmltag = val;
                System.out.println("xml tag: "+xmltag);
            }
            else if (key.equalsIgnoreCase("starttag")) {
                netag1 = val;
                System.out.println("starttag: "+ netag1);
            }
            else if (key.equalsIgnoreCase("endtag")) {
                netag2 = val;
                System.out.println("endtag: "+ netag2);
            }
            else {
                stanfordProps.setProperty(key, val);
            }
        }


        //set xmltag(s)
        ArrayList<String> xmltags = new ArrayList<String>();
        String[] xmlt = xmltag.split(";");
        for(String tags : xmlt){
            System.out.println("xmltags: "+tags);
            xmltags.add(tags);
        }


        ImpactCRFClassifier crf = new ImpactCRFClassifier(stanfordProps);
        crf.trainFormat = inputFormat;

        if(useSpelVar){
            crf.initSpelVarModuleForTraining(stanfordProps, svPropsHM, xmltags, netag1, netag2);
        }
        crf.train(crf, xmltags, netag1, netag2);
        String serializeTo = crf.flags.serializeTo;
        String serializeToText = crf.flags.serializeToText;
        if (serializeTo != null) {
            crf.serializeClassifier(serializeTo);
        }
        if (serializeToText != null) {
            crf.serializeTextClassifier(serializeToText);
        }
    }



    public static void initExtracting(String[] args, boolean spelvar,
                                      ConcurrentHashMap<String, String> svPropsHM, String printSVList,
                                      String printNEList, String inputFormat,
                                      String outputFormat, String xmltag, String netag1, String netag2) throws Exception {

		/*	
		 * 	This method is called from the main NERT-class using flag '-e'.
		 * 	It loads the classifier, sets the input and outputformats and 
		 * 	calls the main extract() method for the file(s).
		 * 
		 * 	The input format of the file(s) to be extracted is set according 
		 * 	to the flag 'in'. The default format is 'bio'. The output format
		 * 	is set by the flag 'out'. If this flag is not specified, the same
		 * 	format as the input flag 'in' is used.
		 * 
		 * 	Finally, the method printNElist() is called to print out all 
		 * 	extracted NE's to the file specified in the flag 'printNElist'
		 */
        System.out.println("Init extracting. \ninputFormat: "+inputFormat+" \noutputFormat: "+outputFormat);
        if(inputFormat.equals("")){System.err.println("No input format specified. Using BIO.");}
        if(outputFormat.equals("")){System.err.println("No output format specified. Using BIO.");}

        //set xmltag
        ArrayList<String> xmltags = new ArrayList<String>();
        String[] xmlt = xmltag.split(";");
        for(String tags : xmlt){
            if( !tags.isEmpty()){
                System.out.println("xmltags: "+tags);
                xmltags.add(tags);
            }
        }

        useSpelVar = spelvar;
        System.out.println("\nReading Stanford arguments ...");
        Properties props = StringUtils.argsToProperties(args);
        ImpactCRFClassifier crf = new ImpactCRFClassifier(props);
        String testFile = crf.flags.testFile;
        String testDirs = crf.flags.testDirs;
        String loadPath = crf.flags.loadClassifier;
        String loadTextPath = crf.flags.loadTextClassifier;
        System.out.println("Done.");
        //	Set outputformat
        if(outputFormat.equals("")){outputFormat = inputFormat;}

        if (loadPath != null) {
            crf.loadClassifierNoExceptions(loadPath, props);
        } else if (loadTextPath != null) {
            try {
                crf.loadTextClassifier(loadTextPath, props);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("error loading " + loadTextPath);
            }
        } else if (crf.flags.loadJarClassifier != null) {
            crf.loadJarClassifier(crf.flags.loadJarClassifier, props);
        }

        if (testDirs != null && testFile == null) {
            File folder = new File(testDirs);
            String absPath = folder.getAbsolutePath();
            File[] listOfFiles = folder.listFiles();
            String[] files = new String[listOfFiles.length];
            System.out.println("Test Files in directory " + absPath + ":");
            for (int i = 0; i < listOfFiles.length; i++) {
                files[i] = absPath + "/" + listOfFiles[i].getName();
                System.out.println("\t" + files[i]);
            }

            for (int i = 0; i < files.length; i++) {
                System.out.println("*** " + files[i] + " ***\n");
                crf.extract(svPropsHM, printSVList, crf,
                        files[i], inputFormat, outputFormat, xmltags, netag1, netag2);
            }
        }

        if (testFile != null) {
            crf.extract(svPropsHM, printSVList, crf,
                    testFile, inputFormat, outputFormat, xmltags, netag1, netag2);
        }

        if ( !printNEList.isEmpty()) {
            nl.inl.impact.ner.io.FileHandler.writeNEsToFile(printNEList, NEListHM);
        }
    }

    public synchronized void extractFromGui(String testFile, String inputFormat, String outputFormat){

        //this method is called with the gui extractor
        //still needs to be properly implemented though..

        //String filecontent = nl.inl.impact.ner.io.FileHandler.readFile(testFile);
        //classifyAndWriteAnswersInTxtOrXml(filecontent, inputFormat, outputFormat, netag);

    }

    private void extract(ConcurrentHashMap<String, String> svPropsHM,
                         String printSVList, ImpactCRFClassifier crf, String testFile, String inputFormat, String outputFormat, ArrayList<String> xmltags, String netag1, String netag2) throws Exception {
		
		/*	This is the main extracting method that is called after initializing
		 * 	in initExtracting(). It runs the spelling variation module and
		 * 	prepares the file(s) to be extracted for the methods classify() and 
		 * 	writeAnswers(). This preparation is not needed when inputformat is 'bio',
		 * 	but it is when the format is 'xml' or 'txt'. In those cases, the current
		 * 	method first reads the (relevant) file input and tokenizes it before 
		 * 	sending it to the method classify().
		 * 
		 * 	Stanford has a readerAndWriter for xml/txt, but for reasons unclear it
		 * 	seemed to underperform when compared to the readerAndWriter for bio. Thus,
		 * 	we, rather unelegantly, read xml/txt, convert it to bio and then use the
		 * 	bio readerAndWriter, adding a dummy 'POS' and 'O' tag to each token.
		 * 
		 *  If inputformat is xml, we only read the text between the tag 'xmltag', which 
		 *  should be specified with the command line flag 'tag'.
		 *  
		 *  Note that, if more than one file is to be extracted, the output is still all sent
		 *  to STDOUT. In order to be able to determine the origin of the output, the output
		 *  of each new file starts with a header with the filename.
		 */

        DocumentReaderAndWriter<IN> readerAndWriter = defaultReaderAndWriter();

        if (useSpelVar) {
            initSpelVarModuleForExtracting(svPropsHM, printSVList, testFile, inputFormat, xmltags, netag1, netag2);
        }

        if (crf.flags.searchGraphPrefix != null) {
            crf.classifyAndWriteViterbiSearchGraph(testFile,
                    crf.flags.searchGraphPrefix, crf.makeReaderAndWriter());
        } else if (crf.flags.printFirstOrderProbs) {
            crf.printFirstOrderProbs(testFile, crf.defaultReaderAndWriter());
        } else if (crf.flags.printFactorTable) {
            crf.printFactorTable(testFile, crf.defaultReaderAndWriter());
        } else if (crf.flags.printProbs) {
            crf.printProbs(testFile, crf.defaultReaderAndWriter());
        } else if (crf.flags.useKBest) {
            int k = crf.flags.kBest;
            crf.classifyAndWriteAnswersKBest(testFile, k, crf.defaultReaderAndWriter());
        } else if (crf.flags.printLabelValue) {
            crf.printLabelInformation(testFile, crf.defaultReaderAndWriter());
        } else {
            if (inputFormat.equalsIgnoreCase("xml")) {
                //System.out.println("FORMAT: XML");
                //read file
                String filecontent = nl.inl.impact.ner.io.FileHandler.readFile(testFile);
                classifyAndWriteAnswersInXML2(filecontent, inputFormat,
                        outputFormat, xmltags, netag1, netag2,
                        readerAndWriter);
                //System.out.println("DONE");
            }
            else if (inputFormat.equalsIgnoreCase("txt")) {
                //read file
                String filecontent = nl.inl.impact.ner.io.FileHandler.readFile(testFile);
                classifyAndWriteAnswersInTxtOrXml(filecontent, inputFormat,
                        outputFormat, netag1, netag2,
                        readerAndWriter);
            }
            else{
                //	Inputformat is not xml or txt, so we assume it's bio
                ArrayList<Integer> emptyEolList = new ArrayList<Integer>();
                ObjectBank<List<CoreLabel>> documents = crf.makeObjectBankFromFile(testFile, readerAndWriter);
                for (List<CoreLabel> doc : documents) {
                    crf.classify(doc);
                    try {
                        crf.writeAnswers(doc, inputFormat, outputFormat, emptyEolList);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private void classifyAndWriteAnswersInTxtOrXml(String filecontent, String inputFormat, String outputFormat, String netag1, String netag2,
                                                   DocumentReaderAndWriter<IN> readerAndWriter) {
		
		/*	This method is called from the main extract() method when
		 * 	input format is txt, or with the relevant chunk from an xml file. 
		 * 	It reads the file(s), tokenizes the text 
		 * 	and then calls classify() and writeAnswers().
		 * 	Like with the xml-format, we convert the tokenized text to the bio-format
		 * 	before classifying, since this seems to be giving better results
		 * 	in the Stanford classifier.
		 * 
		 * 	The method tries to preserve the end-of-line tags as well as 
		 * 	possible. Note, however, that, since the text is tokenized,
		 * 	the output will be slightly different (e.g. with whitespaces
		 * 	around periods, comma's, etc.)
		 * 
		 */

        //System.out.println("TXTorXML: filecontent: "+filecontent);

        //read file
        //String filecontent = nl.inl.impact.ner.io.FileHandler.readFile(testFile);
        //convert to bio
        String s2 = nl.inl.impact.ner.utils.ConvertTxt2Bio.txt2bio(filecontent, netag1, netag2, true, true);
        String EOL = nl.inl.impact.ner.utils.ConvertTxt2Bio.EOL;
        //System.out.println(s2);
        //we now have a string s2 in which the original end of lines are marked
        //with the EOL string. For NER, we do not need these lines, so we iterate over
        //each sentence, take the EOLs out but store their positions, classify them,
        //and use the EOL-positions to reconstruct the txt as well as possible.

        //split on empty lines
        String[] sentences = s2.split("\\s+\\n");
        for(String sentence : sentences){
            //System.out.println("-------------\nSentence: "+sentence);
            String[] words = sentence.split("\\n");
            ArrayList<Integer> EOLpos = new ArrayList<Integer>();
            int wordCounter = 0;
            String s = "";
            for(String w : words){
                //System.out.println("\tw: "+w);
                //for this sentence, take out the EOLs and store their positions
                //in the sentence.
                if(w.indexOf(EOL) == -1){
                    s += w+"\n";
                    wordCounter++;
                }
                else{
                    EOLpos.add(wordCounter);
                }

            }
            //we now have a string s that we classify. Note that it can also be an empty line
            //with just an EOL-mark. In that case we do not classify.
            //System.out.println("sClassifyAndWriteAnswersInTxtOrXml: >"+s+"<");
            if(!s.equals("")){
                ObjectBank<List<IN>> documents = makeObjectBankFromString(s, readerAndWriter);
                for (List<IN> doc : documents) {
                    wordCounter = 0;
					/*
					for(CoreLabel wi : doc){
						System.out.println(wi.getString(CoreAnnotations.TextAnnotation.class) + " " + wi.getString(GoldAnswerAnnotation.class) +" "+ wi.getString(AnswerAnnotation.class));
					}
					*/
                    classify(doc);
                    try {
                        writeAnswers(doc, inputFormat, outputFormat, EOLpos);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            else{
                //empty string: print
                if(EOLpos.size() > 0){
                    //System.out.println("PRINT EMPTY NEWLINE HERE");
                    try {
                        writeAnswers("\n");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        //System.out.println("DONE");

    }

	
	/*	NB I used to have the classifier as argument: ImpactCRFClassifier crf, and use
	 * 	this for calling methods such as writeAnswers();
	 */

    private void classifyAndWriteAnswersInXML2(String filecontent, String inputFormat, String outputFormat,
                                               ArrayList<String> xmltags, String netag1, String netag2,
                                               DocumentReaderAndWriter<IN> readerAndWriter) {
		
		/*	When extracting xml, this method is called. It reads the file, 
		 * 	and classifies only those chunks that are between the tags specified
		 * 	by the user in the String 'xmltag'.
		 * 
		 */

        ArrayList<Integer> emptyEolList = new ArrayList<Integer>();
        ArrayList<String> tags = new ArrayList<String>();
        String s = filecontent;

        String marker = "";
        //System.out.println("xml-tag: "+xmltag);

        if(xmltags.size() == 0){
            System.err.println("WARNING: xml-input without specified xmltag.");
        }


        for(String t : xmltags){
            //strip off <> and add to list 'tags'
            if( t.charAt(0) == '<' && t.charAt(t.length()) == '>' ){
                t = t.substring(1, t.length()-1);
            }
            tags.add(t);
        }


        //strip off <>
        //if( marker.charAt(0) == '<' && marker.charAt(marker.length()) == '>' ){
        //	marker = marker.substring(1, marker.length()-1);
        //}
        System.out.println("new xml-tag: "+marker);

        int onset = -1;
        String sRelevant = "";
        String prev = "";
        //	In steps, we take out the parts of the string that need to be
        //	classified, and print the part before that.

        //we have a list of tags, so we try and find the first one.

        for(String t : tags){
            //int tempOnset = s.indexOf("<" + t + ">");
            int tempOnsetA = s.indexOf(("<" + t));
            int tempOnsetB = s.indexOf(">", tempOnsetA);
            if(onset == -1 || tempOnsetB < onset){
                onset = tempOnsetB;
                marker = t;
            }
        }
        //System.out.println(">>>>First tag found: "+marker+" onset: "+onset+"<<<<");
        int le = 0;
        if(onset > -1){
            //	prev is the part before the found tag, which we will not classify
            //prev = s.substring(0, onset + 2 + marker.length());
            prev = s.substring(0, onset + 1);
            //s = s.substring(onset + 2 + marker.length());
            s = s.substring(onset + 1);
            //le += onset + 2 + marker.length();
            le += onset + 1;
            //System.out.println("le: "+le);
            int offset = s.indexOf("</" + marker + ">");
            le += offset;
            sRelevant = s.substring(0, offset);
            s = s.substring(offset);
            while (onset > -1) {

                //	Print out part of xml before the xmltag:
                try {
                    //System.out.println("PREV: >>>>>>"+prev+"<<<<<<<<");
                    writeAnswers(prev);
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                //System.out.println("SRELEVANT: >>>>>"+sRelevant+"<<<<<<");
                //sRelevant needs to be classified: sent to relevant method
                classifyAndWriteAnswersInTxtOrXml(sRelevant, inputFormat, outputFormat, netag1, netag2, readerAndWriter);

                //System.out.println("Looking for the closest new tag after index le: "+le);
                int tempOnset = -1;
                for(String t : tags){
                    //System.out.println("tempOnset: "+tempOnset);
                    //int tempOnset2 = filecontent.indexOf("<" + t + ">", (onset+offset+2+marker.length()));
                    //int tempOnset2 = filecontent.indexOf("<" + t + ">", le);
                    int tempOnset2a = filecontent.indexOf(("<" + t), le);
                    int tempOnset2b = filecontent.indexOf(">", tempOnset2a);
                    //System.out.println("Found tag >>" +t + "<< at temponset2b: "+tempOnset2b);
                    if(tempOnset == -1 || tempOnset > tempOnset2b){
                        //System.out.println("temponset2b: "+ tempOnset2b +" < tempOnset: " + tempOnset);
                        if(tempOnset2b != -1){
                            tempOnset = tempOnset2b;
                            marker = t;
                            //System.out.println("Changed temponset to: "+tempOnset+" Changed marker to "+marker );
                        }
                    }
                }
                onset = tempOnset;
                //System.out.println("Nieuwe marker??" + marker);
                //onset = s.indexOf("<" + marker + ">");
                String f = "<" + marker;
                //System.out.println("Looking for marker >>"+f+"<< in string s= >>"+s+"<<" );
                int onsetA = s.indexOf(f);
                //System.out.println("found >>"+f+"<< at onsetA: "+onsetA);
                if(onsetA > -1){
                    onset = s.indexOf(">", onsetA);
                    //System.out.println("onset="+onset);
                }
                else{onset = -1;}
                if(onset > -1){
                    //prev = s.substring(0, onset + 2 + marker.length());
                    prev = s.substring(0, onset + 1);
                    //le += onset + 2 + marker.length();
                    le += onset + 1;
                    //System.out.println("Nieuwe prev: >>>>>>>>>"+prev+"<<<<<<<<<<<<");
                    //s = s.substring(onset + 2 + marker.length());
                    s = s.substring(onset + 1);
                    //System.out.println("Nieuwe s: >>>>"+s+"<<<<<<<<<");
                    offset = s.indexOf("</" + marker + ">");
                    le += offset;
                    //System.out.println("Found end marker at offset "+offset+" le="+le);
                    sRelevant = s.substring(0, offset);
                    s = s.substring(offset);
                    //System.out.println("Nieuwe sRelevant= >>"+sRelevant+"<<");
                    //System.out.println("Nieuwe s= >>"+s+"<<");
                }
                else{
//					Print out part of xml before the xmltag:
                    try {
                        //System.out.println("classify: "+s);
                        writeAnswers(s);
                    } catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
                //System.out.println("KLAAR DEZE RONDE. nieuwe marker:"+marker);
            }
        }
        //System.out.println("KLAAR");

    }




    private void initSpelVarModuleForExtracting(
            ConcurrentHashMap<String, String> svPropsHM, String printSVList,
            String testFile, String inputFormat, ArrayList<String> xmltags, String netag1, String netag2) {
		
		/*	This method initializes the spelling variation module
		 * 	when doing NE-extraction. It is called for each file
		 * 	separately, which might not be the most elegant way.
		 *	If the input format is xml or txt, we first have to 
		 *	tokenize the input. For xml, we only consider the text
		 *	between the tags 'xmltag'
		 */

        Timing SVtimer = new Timing();
        System.out.println("\nStarting spelling variation module ...");

        sv.setParameters(svPropsHM);
        sv.loadSpelVarPairs(spelvarlist);
        sv.loadWordsFromFeatureIndex(featureIndex);
        sv.loadGazettesFromModel(gazettes);

        //load testfile into sv
        loadFile2SV(testFile, inputFormat, xmltags, netag1, netag2);


        //run the spelvarmodule

        sv.findSpelVars();

        if (!printSVList.equals("")) {
            sv.writePairListToFile(printSVList, false);
        }
        long millis = SVtimer.stop();
        double secs = ((double) millis) / 1000;
        System.out.println("Duration of spelling variation module: "
                + secs + " seconds.");
        System.out.println("Done.");
        System.out.println("\nRunning Stanford classifier ...");
    }






    // BEGIN special for the KB ***********************************

    public synchronized void setSvPropHM(ConcurrentHashMap<String, String> svPropsHM){
        this.svPropsHM = svPropsHM;
    }

    public synchronized ConcurrentHashMap<String, String> getSvPropHM(){
        return this.svPropsHM;
    }

    public synchronized void initSpelVarModuleForExtracting(String str, ConcurrentHashMap<String, String> svPropsHM) {

        System.out.println("## init SpelVar module for extracting ##");

        sv.setParameters(svPropsHM);
        sv.loadSpelVarPairs(spelvarlist);
        sv.loadWordsFromFeatureIndex(featureIndex);
        sv.loadGazettesFromModel(gazettes);

        sv.loadWordsFromBioString(str, false);

        sv.showWordList();

        //run the spelvarmodule		
        sv.findSpelVars();


    }



    public synchronized void setUseSpelvar(boolean useSpelVar){
        this.useSpelVar = useSpelVar;
    }


    // END special for the KB ***********************************



    public synchronized void initSpelVarModuleForTraining(Properties props, ConcurrentHashMap<String, String> svPropsHM, ArrayList<String> xmltags, String netag1, String netag2) throws IOException{
		
		/*	This method initializes the spelling variation module.
		 * 	It is called from initTraining().
		 * 
		 * 	Using the svProps and the stanfordProps, it loads
		 * 	all relevant words (those from the trainfile(s) and 
		 * 	gazettes) into a spelvar object. Next, it will start
		 * 	building spelvar-pairs that are later used when
		 * 	Stanford starts training.
		 * 
		 * 	If the trainfile-format is 'txt', the input first
		 * 	needs to tokenized. For this, the class 'simpleTok' is 
		 * 	used.
		 */

        useSpelVar = true;
        System.out.println("\nStarting spelling variation module ...");
        Timing SVtimer = new Timing();

        sv.setParameters(svPropsHM);	//also loads the phonetic trans. rules
		
		/*
		 * 	Gazettes.
		 * 	The gazettes are also stored in a list in the crf,
		 *	so we can serialize them and we can access
		 *	them when extracting
		 */

        if (flags.useGazettes && flags.sloppyGazette) {
            for (String filen : flags.gazettes) {
                writeGazettesToList(filen);
                sv.loadWordsFromFile(filen, true);
            }
        }


        // trainFile
        if (flags.trainFile != null) {
            loadFile2SV(flags.trainFile, trainFormat, xmltags, netag1, netag2);

        }
        // trainFiles
        if (flags.trainFiles != null) {
            String[] files = flags.trainFiles.split(";");
            for (int i = 0; i < files.length; i++) {
                //load file
                System.out.println("File: >"+files[i]+"<");
                loadFile2SV(files[i], trainFormat, xmltags, netag1, netag2);
            }
        }
        // trainDirs
        if (flags.trainDirs != null) {
            System.out.println("Scanning directory >"+flags.trainDirs+"<");
            File folder = new File(flags.trainDirs);
            String absPath = folder.getAbsolutePath();
            File[] listOfFiles = folder.listFiles();
            String[] files = new String[listOfFiles.length];
            for (int i = 0; i < listOfFiles.length; i++) {
                files[i] = absPath + "/" + listOfFiles[i].getName();
                System.out.println("Found file: >"+files[i]+"<");
                //load file
                loadFile2SV(files[i], trainFormat, xmltags, netag1, netag2);
            }
        }

        sv.findSpelVars();
        long millis = SVtimer.stop();
        double secs = ((double) millis) / 1000;
        System.out.println("Duration of spelling variation module: "
                + secs + " seconds.");
        System.out.println("Done.\n");
        if (!sv.printSpelVarPairs.equals("")) {
            sv.writePairListToFile("", true);
        }
    }


    private void loadFile2SV(String file, String format, ArrayList<String> xmltags, String netag1, String netag2){
        //load file
        String str = nl.inl.impact.ner.io.FileHandler.readFile(file);
        if(format.equals("txt")){
            //convert to bio
            String sBio = nl.inl.impact.ner.utils.ConvertTxt2Bio.txt2bio(str, netag1, netag2, false, true);
            sv.loadWordsFromBioString(sBio, false);
            //sv.loadWordsFromTxtFile(flags.trainFile);
        }
        else if(format.equals("xml")){
            //we first get the proper parts of the xml file, in txt format
            String sTxt = chopXML(str, xmltags);
            //we then convert the txt to bio
            String sBio = nl.inl.impact.ner.utils.ConvertTxt2Bio.txt2bio(sTxt, netag1, netag2, false, true);
            sv.loadWordsFromBioString(sBio, false);
        }
        else{
            sv.loadWordsFromBioString(str, false);
            //sv.loadWordsFromFile(flags.trainFile, false);
        }
    }



    /** The main method. See the class documentation. */
    public static void main(String[] args) throws Exception {
        StringUtils.printErrInvocationString("CRFClassifier", args);

        Properties props = StringUtils.argsToProperties(args);
        ImpactCRFClassifier<CoreLabel> crf = new ImpactCRFClassifier<CoreLabel>(props);
        String testFile = crf.flags.testFile;
        String testFiles = crf.flags.testFiles;
        String textFile = crf.flags.textFile;
        String textFiles = crf.flags.textFiles;
        String loadPath = crf.flags.loadClassifier;
        String loadTextPath = crf.flags.loadTextClassifier;
        String serializeTo = crf.flags.serializeTo;
        String serializeToText = crf.flags.serializeToText;

        if (loadPath != null) {
            crf.loadClassifierNoExceptions(loadPath, props);
        } else if (loadTextPath != null) {
            try {
                crf.loadTextClassifier(loadTextPath, props);
                // System.err.println("DEBUG: out from crf.loadTextClassifier");
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("error loading " + loadTextPath);
            }
        } else if (crf.flags.loadJarClassifier != null) {
            crf.loadJarClassifier(crf.flags.loadJarClassifier, props);
        } else if (crf.flags.trainFile != null
                || crf.flags.trainFileList != null) {
            crf.train();
        } else {

            crf.loadDefaultClassifier();
        }

        if (serializeTo != null) {
            crf.serializeClassifier(serializeTo);
        }

        if (serializeToText != null) {
            crf.serializeTextClassifier(serializeToText);
        }

        if (testFile != null) {
            DocumentReaderAndWriter<CoreLabel> readerAndWriter = crf.defaultReaderAndWriter();
            if (crf.flags.searchGraphPrefix != null) {
                crf.classifyAndWriteViterbiSearchGraph(testFile, crf.flags.searchGraphPrefix,
                        crf.makeReaderAndWriter());
            } else if (crf.flags.printFirstOrderProbs) {
                crf.printFirstOrderProbs(testFile, readerAndWriter);
            } else if (crf.flags.printFactorTable) {
                crf.printFactorTable(testFile, readerAndWriter);
            } else if (crf.flags.printProbs) {
                crf.printProbs(testFile, readerAndWriter);
            } else if (crf.flags.useKBest) {
                int k = crf.flags.kBest;
                crf.classifyAndWriteAnswersKBest(testFile, k, readerAndWriter);
            } else if (crf.flags.printLabelValue) {
                crf.printLabelInformation(testFile, readerAndWriter);
            } else {
                crf.classifyAndWriteAnswers(testFile);
            }

            // crf.printProbs(testFile); //original code

        }

        if (testFiles != null) {
            List<File> files = new ArrayList<File>();
            for (String filename : testFiles.split(",")) {
                files.add(new File(filename));
            }
            crf.classifyFilesAndWriteAnswers(files, crf.defaultReaderAndWriter());
        }

        if (textFile != null) {
            crf.classifyAndWriteAnswers(textFile);
        }

        if (textFiles != null) {
            List<File> files = new ArrayList<File>();
            for (String filename : textFiles.split(",")) {
                files.add(new File(filename));
            }
            crf.classifyFilesAndWriteAnswers(files);
        }

        if (crf.flags.readStdin) {
            crf.classifyStdin();
        }
    }





    private String chopXML(String filecontent, ArrayList<String> xmltags) {
		
		/*	When training with xml, this method is called. It reads the string,
		 *  and removes all irrelevant parts of the xml, leaving only those chunks
		 * 	that is trained with. These parts are turned into 'bio' format and
		 *	returned as a single string.
		 * 
		 */

        ArrayList<String> tags = new ArrayList<String>();
        String s = filecontent;
        StringBuilder sb = new StringBuilder();

        String marker = "";
        //System.out.println("xml-tag: "+xmltag);

        if(xmltags.size() == 0){
            System.err.println("WARNING: xml-input without specified xmltag.");
        }
        for(String t : xmltags){
            //strip off <> and add to list 'tags'
            if( t.charAt(0) == '<' && t.charAt(t.length()) == '>' ){
                t = t.substring(1, t.length()-1);
            }
            tags.add(t);
        }


        int onset = -1;
        String sRelevant = "";
        String prev = "";
        //	In steps, we take out the parts of the string that need to be
        //	classified, and print the part before that.

        //we have a list of tags, so we try and find the first one.

        for(String t : tags){
            //this only works if the tag is indeed <tag>, not if we have <tag attr="false">
            //so we only look for <tag instead of <tag>, and then for the endtag.
            int tempOnsetA = s.indexOf("<" + t);
            int tempOnsetB = s.indexOf(">", tempOnsetA);
            if(onset == -1 || tempOnsetB < onset){
                onset = tempOnsetB;
                marker = t;
            }
        }
        //System.out.println(">>>>First tag found: "+marker+" onset: "+onset+"<<<<");
        int le = 0;
        if(onset > -1){
            //	prev is the part before the found tag, which we will not classify
            //prev = s.substring(0, onset + 2 + marker.length());
            prev = s.substring(0, onset + 1);	//including the start tag itself
            //s = s.substring(onset + 2 + marker.length());
            s = s.substring(onset + 1);
            //le += onset + 2 + marker.length();
            le += onset + 1;
            int offset = s.indexOf("</" + marker + ">");
            le += offset;
            sRelevant = s.substring(0, offset);
            s = s.substring(offset);
            while (onset > -1) {

                //	prev is irrelevant part

                //System.out.println("SRELEVANT: >>>>>"+sRelevant+"<<<<<<");
                // sRelevant is the relevant part: add to string
                sb.append(sRelevant);

                //System.out.println("Looking for the closest new tag after index le: "+le);
                int tempOnset = -1;
                for(String t : tags){
                    //System.out.println("tempOnset: "+tempOnset);
                    //int tempOnset2 = filecontent.indexOf("<" + t + ">", (onset+offset+2+marker.length()));

                    //int tempOnset = filecontent.indexOf("<" + t + ">", le);
                    int tempOnsetA = filecontent.indexOf("<" + t, le);
                    int tempOnsetB = filecontent.indexOf(">", tempOnsetA);
                    //System.out.println("Found tag " +t + " at temponset2: "+tempOnset2);
                    if(tempOnsetB == -1 || tempOnset > tempOnsetB){
                        //System.out.println("temponset2: "+ tempOnset2 +" < tempOnset: " + tempOnset);
                        if(tempOnsetB != -1){
                            tempOnset = tempOnsetB;
                            marker = t;
                            //System.out.println("Changed temponset to: "+tempOnset+" Changed marker to "+marker );
                        }
                    }
                }
                onset = tempOnset;
                //System.out.println("Nieuwe marker??" + marker);
                //onset = s.indexOf("<" + marker + ">");
                //System.out.println("Zoek naar begin en eind van marker "+marker);
                //System.out.println("Zoek in string s >>"+s+"<<");
                int onsetA = s.indexOf("<" + marker);
                if(onsetA > -1){
                    onset = s.indexOf(">", onsetA);
                }
                //System.out.println("Marker gevonden op onset: "+onset + " with onsetA: "+onsetA);

                if(onset > -1){
                    //prev = s.substring(0, onset + 2 + marker.length());
                    prev = s.substring(0, onset + 1);
                    //le += onset + 2 + marker.length();
                    le += onset + 1;
                    //System.out.println("Nieuwe prev: >>>>>>>>>"+prev+"<<<<<<<<<<<<");
                    //s = s.substring(onset + 2 + marker.length());
                    s = s.substring(onset + 1);
                    offset = s.indexOf("</" + marker + ">");
                    le += offset;
                    //System.out.println("lengte s: "+s.length()+" offset: "+offset);
                    sRelevant = s.substring(0, offset);
                    s = s.substring(offset);
                    //System.out.println("Nieuwe s = >>"+s+"<<");
                    //System.out.println("Nieuwe srelevant = >>"+sRelevant+"<<");
                }
                else{
//					Print out part of xml before the xmltag:
                    //irrelevant
                }
                //System.out.println("KLAAR DEZE RONDE. nieuwe marker:"+marker);
            }
        }
        //System.out.println("KLAAR");
        //System.out.println("Trainen met: "+returnString);

        //if(useSpelVar){
        //	sv.loadWordsFromBioString(returnString, false);
        //}
        //String returnString = nl.inl.impact.ner.utils.ConvertTxt2Bio.txt2bio(sb.toString(), false, true);

        return sb.toString();
        //return makeObjectBankFromTxt(returnString);
        //return nl.inl.impact.ner.utils.ConvertTxt2Bio.txt2bio(returnString, false, true);
    }


    public synchronized ObjectBank<List<IN>> makeObjectBankFromTxt(String str, String netag1, String netag2, DocumentReaderAndWriter<IN> readerAndWriter){

		/*	If we're training with a text file, we first turn the txt-format
		 * 	into bio, and use this to fill the object bank.
		 */
        String s = nl.inl.impact.ner.utils.ConvertTxt2Bio.txt2bio(str, netag1, netag2, false, true);

        ObjectBank<List<IN>> documents = makeObjectBankFromString(s, readerAndWriter);

        return documents;
    }




    public synchronized void train(ImpactCRFClassifier crf, ArrayList<String> xmltags, String netag1, String netag2) {
		
		/*	The actual method that starts the Stanford train method.
		 * 
		 * 	Trainfiles can be txt, xml or bio.
		 * 	In the case of txt, we convert the txt-
		 * 	format to bio. In the case of xml, we do the same, but
		 * 	first remove those chunks that are outside the xmltag(s)
		 * 	specified by the user in the properties file.
		 * 
		 *  If applicable, spelvar has been initalized and, if applicable,
		 *  gazetteers have been loaded. The words from the trainfile are
		 *  added to the spelvar lists here, because they are converted from
		 *  xml or txt to bio only in this method.
		 */

        if (crf.flags.trainFiles != null) {
            String[] files = crf.flags.trainFiles.split(";");
            if(trainFormat.equals("txt") || trainFormat.equals("xml")){
                for(String file : files){

                    System.out.println("Processing "+file);

                    String s = nl.inl.impact.ner.io.FileHandler.readFile(file);
                    //if useSpelVar == true, the words are loaded in the called methods
                    //makeObjectBank
                    if(trainFormat.equals("xml")){
                        //for training, we first get the proper parts of the xml file, in txt format
                        String sTxt = chopXML(s, xmltags);
                        //we then convert the txt to bio
                        String sBio = nl.inl.impact.ner.utils.ConvertTxt2Bio.txt2bio(sTxt, netag1, netag2, false, true);


                        //we then convert the bio in an objectbank
                        //ObjectBank<List<CoreLabel>> obj = makeObjectBankFromXml(s, xmltags);
                        ObjectBank<List<IN>> obj = makeObjectBankFromString(sBio, defaultReaderAndWriter());
                        //if(useSpelVar){doSpelVar();}
                        train(obj);
                    }
                    else{
                        ObjectBank<List<IN>> obj = makeObjectBankFromTxt(s, netag1, netag2, defaultReaderAndWriter());
                        //if(useSpelVar){doSpelVar();}
                        train(obj);
                    }
                }
            }
            else{
				/*
				//init spelvar here
				if(useSpelVar){
					for(String file: files){
						sv.loadWordsFromFile(file, false);
					}
					doSpelVar();
				}
				*/
                train(files);
            }
        }
        else if (crf.flags.trainDirs != null) {
            File folder = new File(crf.flags.trainDirs);
            String absPath = folder.getAbsolutePath();
            File[] listOfFiles = folder.listFiles();
            String[] files = new String[listOfFiles.length];
            System.out.println("TrainFiles in directory " + absPath + ":");
            for (int i = 0; i < listOfFiles.length; i++) {
                files[i] = absPath + "/" + listOfFiles[i].getName();
                System.out.println("\t" + files[i]);
            }
            if(trainFormat.equals("txt") || trainFormat.equals("xml")){
                for(String file : files){
                    String s = nl.inl.impact.ner.io.FileHandler.readFile(file);
                    //init spelvar in the makeObjectBank methods
                    if(trainFormat.equals("xml")){
                        //for training, we first get the proper parts of the xml file, in txt format
                        String sTxt = chopXML(s, xmltags);
                        //we then convert the txt to bio
                        String sBio = nl.inl.impact.ner.utils.ConvertTxt2Bio.txt2bio(sTxt, netag1, netag2, false, true);
                        //we then convert the bio in an objectbank


                        //ObjectBank<List<CoreLabel>> obj = makeObjectBankFromXml(s, xmltags);
                        ObjectBank<List<IN>> obj = makeObjectBankFromString(sBio, defaultReaderAndWriter());
                        //ObjectBank<List<CoreLabel>> obj = makeObjectBankFromXml(s, xmltags);
                        //if(useSpelVar){doSpelVar();}
                        train(obj);
                    }
                    else{
                        ObjectBank<List<IN>> obj = makeObjectBankFromTxt(s, netag1, netag2, defaultReaderAndWriter());
                        //if(useSpelVar){doSpelVar();}
                        train(obj);
                    }
                }
            }
			/*
			else if(trainFormat.equals("xml")){
				for(String file : files){
					train(makeObjectBankFromXmlFile(file));
				}
			*/
            else{
				/*
				//init spelvar here
				if(useSpelVar){
					for(String file: files){
						sv.loadWordsFromFile(file, false);
					}
				}
				*/
                train(files);
            }
        } else {
            if(trainFormat.equals("txt") || trainFormat.equals("xml")){
                String s = nl.inl.impact.ner.io.FileHandler.readFile(crf.flags.trainFile);
                //init spelvar in the makeObjectBank methods
                if(trainFormat.equals("xml")){
                    //System.out.println("Alhier met string s "+s.length());
                    //ObjectBank<List<CoreLabel>> obj = makeObjectBankFromXml(s, xmltags);
                    //for training, we first get the proper parts of the xml file, in txt format
                    String sTxt = chopXML(s, xmltags);
                    //we then convert the txt to bio
                    String sBio = nl.inl.impact.ner.utils.ConvertTxt2Bio.txt2bio(sTxt, netag1, netag2, false, true);
                    //we then convert the bio in an objectbank
                    ObjectBank<List<IN>> obj = makeObjectBankFromString(sBio, defaultReaderAndWriter());
                    //ObjectBank<List<CoreLabel>> obj = makeObjectBankFromXml(s, xmltags);
                    train(obj);
                }
                else{
                    ObjectBank<List<IN>> obj = makeObjectBankFromTxt(s, netag1, netag2, defaultReaderAndWriter());
                    train(obj);
                }
            }
			/*
			else if(trainFormat.equals("xml")){
				train(makeObjectBankFromXmlFile(crf.flags.trainFile));
			}*/
            else{
				/*
				if(useSpelVar){
					sv.loadWordsFromFile(crf.flags.trainFile, false);
				}
				*/
                train(crf.flags.trainFile);
            }
        }
    }





    /**
     * Classify the contents of a {@link String}. Plain text or XML input is
     * expected and the {@link PlainTextDocumentReaderAndWriter} is used. The
     * classifier will tokenize the text and treat each sentence as a separate
     * document. The output can be specified to be in a choice of three formats:
     * slashTags (e.g., Bill/PERSON Smith/PERSON died/O ./O), inlineXML (e.g.,
     * &lt;PERSON&gt;Bill Smith&lt;/PERSON&gt; went to
     * &lt;LOCATION&gt;Paris&lt;/LOCATION&gt; .), or xml, for stand-off XML
     * (e.g., &lt;wi num="0" entity="PERSON"&gt;Sue&lt;/wi&gt; &lt;wi num="1"
     * entity="O"&gt;shouted&lt;/wi&gt; ). There is also a binary choice as to
     * whether the spacing between tokens of the original is preserved or
     * whether the (tagged) tokens are printed with a single space (for
     * inlineXML or slashTags) or a single newline (for xml) between each one.
     * <p>
     * <i>Fine points:</i> The slashTags and xml formats show tokens as
     * transformed by any normalization processes inside the tokenizer, while
     * inlineXML shows the tokens exactly as they appeared in the source text.
     * When a period counts as both part of an abbreviation and as an end of
     * sentence marker, it is included twice in the output String for slashTags
     * or xml, but only once for inlineXML, where it is not counted as part of
     * the abbreviation (or any named entity it is part of). For slashTags with
     * preserveSpacing=true, there will be two successive periods such as "Jr.."
     * The tokenized (preserveSpacing=false) output will have a space or a
     * newline after the last token.
     *
     * @param sentences
     *            The String to be classified. It will be tokenized and divided
     *            into documents according to (heuristically determined)
     *            sentence boundaries.
     * @param outputFormat
     *            The format to put the output in: one of "slashTags", "xml", or
     *            "inlineXML"
     * @param preserveSpacing
     *            Whether to preserve the input spacing between tokens, which
     *            may sometimes be none (true) or whether to tokenize the text
     *            and print it with one space between each token (false)
     * @return A {@link String} with annotated with classification information.
     */
    // OVERRIDE THESE FROM AbstractSequenceClassifier

    // new 2013
    public synchronized String classifyToString(String sentences, String outputFormat,
                                                boolean preserveSpacing) {

        PlainTextDocumentReaderAndWriter.OutputStyle outFormat =
                PlainTextDocumentReaderAndWriter.OutputStyle.fromShortName(outputFormat);

        ObjectBank<List<IN>> documents =
                makeObjectBankFromString(sentences, plainTextReaderAndWriter());

        int counter = 0;
        StringBuilder sb = new StringBuilder();
        for (List<IN> doc : documents) {
            List<IN> docOutput = classify(doc);
            if (plainTextReaderAndWriter() instanceof PlainTextDocumentReaderAndWriter) {
                // TODO: implement this particular method and its options in
                // the other documentReaderAndWriters
                labeledSentences.put(counter,
                        ((PlainTextDocumentReaderAndWriter<IN>) plainTextReaderAndWriter()).getAnswers(docOutput, outFormat, preserveSpacing));
                sb.append(((PlainTextDocumentReaderAndWriter<IN>) plainTextReaderAndWriter()).getAnswers(docOutput, outFormat, preserveSpacing));
            } else {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                plainTextReaderAndWriter().printAnswers(docOutput, pw);
                pw.flush();
                sb.append(sw.toString());
                sb.append("\n");
            }
            counter++;
        }
        return sb.toString();
    }

    public synchronized ConcurrentHashMap<Integer, String> getLabeledSentences() {
        return labeledSentences;
    }

    /**
     * Classify the contents of a {@link String}. Plain text or XML is expected
     * and the {@link PlainTextDocumentReaderAndWriter} is used. The classifier
     * will treat each sentence as a separate document. The output can be
     * specified to be in a choice of formats: Output is in inline XML format
     * (e.g. &lt;PERSON&gt;Bill Smith&lt;/PERSON&gt; went to
     * &lt;LOCATION&gt;Paris&lt;/LOCATION&gt; .)
     *
     * @param sentences
     *            The string to be classified
     * @return A {@link String} with annotated with classification information.
     */
    public synchronized String classifyWithInlineXML(String sentences) {
        return classifyToString(sentences, "inlineXML", true);
    }

    /**
     * Classify the contents of a String to a tagged word/class String. Plain
     * text or XML input is expected and the
     * {@link PlainTextDocumentReaderAndWriter} is used. Output looks like: My/O
     * name/O is/O Bill/PERSON Smith/PERSON ./O
     *
     * @param sentences
     *            The String to be classified
     * @return A String annotated with classification information.
     */
    public synchronized String classifyToString(String sentences) {
        return classifyToString(sentences, "slashTags", true);
    }

    private void writeGazettesToList(String file) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(file));
        Pattern p = Pattern.compile("^(\\S+)\\s+(.+)$");
        for (String line; (line = r.readLine()) != null;) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                // String type = m.group(1);
                String phrase = m.group(2);
                gazettes.add(phrase);
            }
        }
        r.close();
    }

    private void convertSVHMToSVList() {
        ConcurrentHashMap<String, String> spelvarHM = sv.spelvarHM;
        for (String a : spelvarHM.keySet()) {
            spelvarlist.add(a + "@@@@" + sv.spelvarHM.get(a));
        }
    }



    // store Named Entities into hash for later export
    public static void storeNEs(String w, String ans) {

        String total = ans + " " + w;
        if (NEListHM.containsKey(total)) {
            int v = NEListHM.get(total);
            v++;
            NEListHM.put(total, v);
        } else {
            NEListHM.put(total, 1);
        }
    }


    @Override
    public synchronized List<IN> classifyWithGlobalInformation(List<IN> tokenSeq, final CoreMap doc, final CoreMap sent) {
        return classify(tokenSeq);
    }

    public static void storeText(String s, String tag) {

        if (s.equals("X")) {
			/* End of sentence: add line break */
            String t = "\n";
            textToXML.put(storeTextCounter, t);
            storeTextCounter++;
        } else {
			/* Output: <tag>text</tag> */
            // if(!tag.equals("O")){tag = "NE_" + tag;}
            String t = NERTStringUtils.checkForXML(s);
            t = "<" + tag + ">" + t + "</" + tag + "> ";
            textToXML.put(storeTextCounter, t);
            storeTextCounter++;
        }

        if (!usedTags.contains(tag) && !tag.equals("O") && !tag.equals("X")) {
            usedTags.add(tag);
        }
    }

    public static void writeTextToXMLFile(String file) {

        FileOutputStream out;
        PrintStream p;

        String meta4 = "\t<CODEEXAMPLE>";
        for (String t : usedTags) {
            meta4 += "<" + t + ">" + t + "</" + t + ">/";
        }
        meta4 = meta4.substring(0, meta4.length() - 1);
        meta4 += "</CODEEXAMPLE>\n\t<TEXT>";
        String metaBegin = meta1 + file + meta2 + getDateTime() + meta3 + meta4;
        String metaEnd = "\t</TEXT>\n</FILE>";

        try {
            out = new FileOutputStream(file);
            p = new PrintStream(out);
            p.println(metaBegin);
            Iterator itr = textToXML.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry pairs = (Map.Entry) itr.next();
                p.print(pairs.getValue());
            }
            p.println(metaEnd);
            p.close();
            System.out.println("Written tagged text to XML file " + file);
        } catch (Exception e) {
            System.err.println("Error writing tagged text to XML file " + e);
        }

        // we also add css file to same directory as xml file above.
        // for this, we first have to set the colors for all the used tags.

        String style = "FILEINFO{color: #000099;font-size: 14pt;display: block;margin-bottom: 20px;}SOURCEFILE{display: block;}DATE{display: block;}CODEEXAMPLE{font-size: 14pt;display: block;margin-bottom: 20pt;}TEXT{background-color: #FFFFFF;width: 100%;}O{color: black;font-size: 15pt;}";
        String[] colors = getTagColors(usedTags.size());

        int tel = 0;
        for (String a : usedTags) {
            style += a + "{color: black;background-color: ";
            style += colors[tel];
            style += ";font-size: 15pt;}";
            tel++;
        }

        int path = file.lastIndexOf("/");
        if (path > 0) {
            file = file.substring(0, path) + "/styleNERT.css";
        } else {
            file = "styleNERT.css";
        }

        try {
            out = new FileOutputStream(file);
            p = new PrintStream(out);
            p.println(style);
            p.close();
            System.out.println("Written css to file " + file);
        } catch (Exception e) {
            System.err.println("Error writing css to file " + e);
        }

    }

    private static String[] getTagColors(int num) {

        String[] col = new String[num];

        // first 4 colors are fixed
        for (int i = 0; i < num; i++) {
            if (i == 0) {
                col[i] = "#ccFF00";
            } // green
            if (i == 1) {
                col[i] = "#ccccFF";
            } // light purple
            if (i == 2) {
                col[2] = "#FF9900";
            } // orange
            if (i == 3) {
                col[3] = "#FFFF99";
            } // yellow
        }
        // if we need more than 4 colors, we create them randomly
        if (num > 4) {
            Random rdm = new Random();

            String[] c = { "00", "33", "99", "cc", "FF" };
            for (int i = 4; i < num; i++) {
                int tel = 0;
                col[i] = "#";
                for (int j = 0; j < 3; j++) {
                    col[i] += c[rdm.nextInt(5)];
                }
                boolean ok = true;
                for (int k = 0; k < i - 1; k++) {
                    if (col[i].equals(col[k])) {
                        ok = false;
                    }
                }
                while (!ok && tel < 10) {
                    col[i] = "#";
                    for (int j = 0; j < 3; j++) {
                        col[i] += c[rdm.nextInt(5)];
                    }
                    ok = true;
                    for (int k = 0; k < i - 1; k++) {
                        if (col[i].equals(col[k])) {
                            ok = false;
                        }
                    }
                    tel++; // to prevent the loop from getting stuck..
                }
            }
        }

        return col;
    }

    private static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }





    // added, missing in 2013
    public synchronized void train(String[] filenames) {
        train(filenames, defaultReaderAndWriter());
    }


} // end class CRFClassifier

