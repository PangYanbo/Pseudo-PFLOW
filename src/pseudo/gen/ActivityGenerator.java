package pseudo.gen;

import java.io.*;
import java.util.*;

import jp.ac.ut.csis.pflow.routing4.res.Network;
import org.opengis.referencing.FactoryException;
import pseudo.acs.CensusODAccessor;
import pseudo.acs.DataAccessor;
import pseudo.acs.MNLParamAccessor;
import pseudo.acs.MkChainAccessor;
import pseudo.acs.PersonAccessor;
import pseudo.acs.SchoolRefAccessor;
import pseudo.res.EGender;
import pseudo.res.ELabor;
import pseudo.res.EMarkov;
import pseudo.res.HouseHold;
import pseudo.res.Country;

/**
 * Unified mainline activity generator.
 * Runs commuter, non-commuter, and student activity generation in a single pass
 * and writes combined output to activity/{pref}/person_{city}.csv.
 *
 * For debugging individual labor types, use the specialized generators:
 * CommuterActivityGenerator, NonCommuterActivityGenerator, StudentActivityGenerator.
 */
public class ActivityGenerator {

	private static final ELabor[] STUDENT_LABORS = {
		ELabor.PRE_SCHOOL, ELabor.PRIMARY_SCHOOL, ELabor.SECONDARY_SCHOOL,
		ELabor.HIGH_SCHOOL, ELabor.JUNIOR_COLLEGE, ELabor.COLLEGE
	};

	public static void main(String[] args) throws IOException, FactoryException {

		System.out.println("ActivityGenerator: start");
		long starttime = System.currentTimeMillis();

		// load config
		InputStream inputStream = ActivityGenerator.class.getClassLoader()
				.getResourceAsStream("config.properties");
		if (inputStream == null) {
			throw new FileNotFoundException("config.properties file not found in the classpath");
		}
		Properties prop = new Properties();
		prop.load(inputStream);

		String root = prop.getProperty("root");
		String inputDir = prop.getProperty("inputDir");
		String output = prop.getProperty("outputDir", root);
		System.out.println("Root Directory: " + root);
		System.out.println("Input Directory: " + inputDir);

		// load shared reference data
		Country country = new Country();

		String stationFile = String.format("%sbase_station.csv", inputDir);
		Network station = DataAccessor.loadLocationData(stationFile);
		country.setStation(station);

		String cityFile = String.format("%scity_boundary.csv", inputDir);
		DataAccessor.loadCityData(cityFile, country);

		String censusFile = String.format("%scity_census_od.csv", inputDir);
		CensusODAccessor odAcs = new CensusODAccessor(censusFile, country);

		String hospitalFile = String.format("%scity_hospital.csv", inputDir);
		DataAccessor.loadHospitalData(hospitalFile, country);

		String restaurantFile = String.format("%scity_restaurant.csv", inputDir);
		DataAccessor.loadRestaurantData(restaurantFile, country);

		String retailFile = String.format("%scity_retail.csv", inputDir);
		DataAccessor.loadRetailData(retailFile, country);

		// student-specific: preschool + school data
		String preschoolFile = String.format("%scity_pre_school.csv", inputDir);
		DataAccessor.loadPreSchoolData(preschoolFile, country);

		String schoolFile = String.format("%scity_school.csv", inputDir);
		DataAccessor.loadSchoolData(schoolFile, country);

		String meshFile = String.format("%smesh_ecensus.csv", inputDir);
		DataAccessor.loadEconomicCensus(meshFile, country);

		// load data after economic census
		String tatemonFile = String.format("%scity_tatemono.csv", inputDir);
		DataAccessor.loadZenrinTatemono(tatemonFile, country, 1);

		// load MNL parameters for all labor types
		MNLParamAccessor mnlAcsCommuter = new MNLParamAccessor();
		mnlAcsCommuter.add(String.format("%s/mnl/labor_params.csv", inputDir), ELabor.WORKER);

		MNLParamAccessor mnlAcsNonCommuter = new MNLParamAccessor();
		mnlAcsNonCommuter.add(String.format("%s/mnl/nolabor_params.csv", inputDir), ELabor.NO_LABOR);

		MNLParamAccessor mnlAcsStudent = new MNLParamAccessor();
		String mnlFile1 = String.format("%s/mnl/student1_params.csv", inputDir);
		mnlAcsStudent.add(mnlFile1, ELabor.PRE_SCHOOL);
		mnlAcsStudent.add(mnlFile1, ELabor.PRIMARY_SCHOOL);
		mnlAcsStudent.add(mnlFile1, ELabor.SECONDARY_SCHOOL);
		String mnlFile2 = String.format("%s/mnl/student2_params.csv", inputDir);
		mnlAcsStudent.add(mnlFile2, ELabor.HIGH_SCHOOL);
		mnlAcsStudent.add(mnlFile2, ELabor.JUNIOR_COLLEGE);
		mnlAcsStudent.add(mnlFile2, ELabor.COLLEGE);

		// school reference accessor for student
		SchoolRefAccessor schAcs = new SchoolRefAccessor();

		int mfactor = 1;
		String outputDir = String.format("%s/activity/", output);

		// determine prefecture range from args or default
		int start = 1;
		int end = 47;
		if (args.length >= 1) {
			start = end = Integer.parseInt(args[0]);
		}
		if (args.length >= 2) {
			mfactor = Integer.parseInt(args[1]);
		}

		for (int i = start; i <= end; i++) {
			String prefKey = "pref." + i;
			String prefRelPath = prop.getProperty(prefKey);
			if (prefRelPath == null) {
				System.err.println("Missing config key: " + prefKey + " -- skipping prefecture " + i);
				continue;
			}

			// create output directory
			File prefDir = new File(outputDir, String.valueOf(i));
			prefDir.mkdirs();
			System.out.println("Start prefecture: " + i);

			// --- Commuter markov ---
			Map<EMarkov, Map<EGender, MkChainAccessor>> mrkMapCommuter = new HashMap<>();
			{
				String maleFile = inputDir + prefRelPath + "_trip_labor_male_prob.csv";
				String femaleFile = inputDir + prefRelPath + "_trip_labor_female_prob.csv";
				Map<EGender, MkChainAccessor> map = new HashMap<>();
				map.put(EGender.MALE, new MkChainAccessor(maleFile));
				map.put(EGender.FEMALE, new MkChainAccessor(femaleFile));
				mrkMapCommuter.put(EMarkov.LABOR, map);
			}
			CommuterActivityGenerator commuter = new CommuterActivityGenerator(
					country, mrkMapCommuter, mnlAcsCommuter, odAcs);

			// --- NonCommuter markov ---
			Map<EMarkov, Map<EGender, MkChainAccessor>> mrkMapNonCommuter = new HashMap<>();
			{
				String maleFile = inputDir + prefRelPath + "_trip_nolabor_male_prob.csv";
				String femaleFile = inputDir + prefRelPath + "_trip_nolabor_female_prob.csv";
				Map<EGender, MkChainAccessor> map = new HashMap<>();
				map.put(EGender.MALE, new MkChainAccessor(maleFile));
				map.put(EGender.FEMALE, new MkChainAccessor(femaleFile));
				mrkMapNonCommuter.put(EMarkov.NOLABOR_JUNIOR, map);
			}
			{
				String maleFile = inputDir + prefRelPath + "_trip_nolabor_male_senior_prob.csv";
				String femaleFile = inputDir + prefRelPath + "_trip_nolabor_female_senior_prob.csv";
				Map<EGender, MkChainAccessor> map = new HashMap<>();
				map.put(EGender.MALE, new MkChainAccessor(maleFile));
				map.put(EGender.FEMALE, new MkChainAccessor(femaleFile));
				mrkMapNonCommuter.put(EMarkov.NOLABOR_SENIOR, map);
			}
			NonCommuterActivityGenerator nonCommuter = new NonCommuterActivityGenerator(
					country, mrkMapNonCommuter, mnlAcsNonCommuter);

			// --- Student markov ---
			Map<EMarkov, Map<EGender, MkChainAccessor>> mrkMapStudent = new HashMap<>();
			{
				String maleFile = inputDir + prefRelPath + "_trip_student1_prob.csv";
				Map<EGender, MkChainAccessor> map = new HashMap<>();
				map.put(EGender.MALE, new MkChainAccessor(maleFile));
				mrkMapStudent.put(EMarkov.STUDENT1, map);
			}
			{
				String maleFile = inputDir + prefRelPath + "_trip_student2_prob.csv";
				Map<EGender, MkChainAccessor> map = new HashMap<>();
				map.put(EGender.MALE, new MkChainAccessor(maleFile));
				mrkMapStudent.put(EMarkov.STUDENT2, map);
			}
			StudentActivityGenerator student = new StudentActivityGenerator(
					country, mrkMapStudent, mnlAcsStudent, odAcs, schAcs);

			// iterate city household files
			File householdDir = new File(String.format("%s/agent/", root), String.valueOf(i));
			File[] houseFiles = householdDir.listFiles();
			if (houseFiles == null) {
				System.err.println("Household directory not found: " + householdDir.getAbsolutePath());
				continue;
			}

			String prePref = "";
			for (File file : houseFiles) {
				if (!file.getName().contains(".csv")) continue;

				// load school reference data per-prefecture (student needs this)
				String name = file.getName();
				String pref = name.substring(7, 9);
				if (!prePref.equals(pref)) {
					schAcs.load(String.format("%sschool/primary_%s.csv", inputDir, pref), ELabor.PRIMARY_SCHOOL);
					schAcs.load(String.format("%sschool/secondary_%s.csv", inputDir, pref), ELabor.SECONDARY_SCHOOL);
					prePref = pref;
				}

				// run all three generators on their respective labor subsets
				List<HouseHold> workerHH = PersonAccessor.load(
						file.getAbsolutePath(), new ELabor[]{ELabor.WORKER}, mfactor);
				commuter.assign(workerHH);

				List<HouseHold> nolaborHH = PersonAccessor.load(
						file.getAbsolutePath(), new ELabor[]{ELabor.NO_LABOR}, mfactor);
				nonCommuter.assign(nolaborHH);

				List<HouseHold> studentHH = PersonAccessor.load(
						file.getAbsolutePath(), STUDENT_LABORS, mfactor);
				student.assign(studentHH);

				// write combined output
				List<HouseHold> allHH = new ArrayList<>();
				allHH.addAll(workerHH);
				allHH.addAll(nolaborHH);
				allHH.addAll(studentHH);

				String resultName = String.format("%s%s/%s", outputDir, i, file.getName());
				PersonAccessor.writeActivities(resultName, allHH);
			}

			System.out.println("end prefecture " + i);
			System.out.println("commuter motifs: " + commuter.mapMotif);
			System.out.println("noncommuter motifs: " + nonCommuter.mapMotif);
			System.out.println("student motifs: " + student.mapMotif);
		}

		long endtime = System.currentTimeMillis();
		System.out.println("Total time: " + (endtime - starttime) + " ms");
	}
}
