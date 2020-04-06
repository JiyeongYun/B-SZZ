package hgu.csee.isel.szz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class BSZZRunner {
	private String GIT_URL;
	private List<String> bugFixCommits = new ArrayList<>();
	private boolean help;

	public static void main(String[] args) {
		BSZZRunner bSZZRunner = new BSZZRunner();
		try {
			bSZZRunner.run(args);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Definition Stage
	private Options createOptions() {
		Options options = new Options();

		options.addOption(Option.builder("u")
								.longOpt("url")
								.desc("Git URL (ex. https://github.com/JiyeongYun/B-SZZ)")
								.hasArg()
								.required(true)
								.build());	

		options.addOption(Option.builder("b")
								.longOpt("bfc")
								.desc("Bug Fix Commits")
								.hasArgs()
								.valueSeparator(' ') // delim
								.required(true)
								.build());

		options.addOption(Option.builder("h")
								.longOpt("help")
								.desc("Help")
								.build());

		return options;
	}

	// Parsing Stage
	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);

			GIT_URL = cmd.getOptionValue('u');
			Collections.addAll(bugFixCommits, cmd.getOptionValues('b'));
			help = cmd.hasOption('h');

		} catch (ParseException e) {
			printHelp(options);
			return false;
		}

		return true;
	}

	// Interrogation Stage
	private void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();

		String header = "B-SZZ finds lines that introduce bug for given BFC.\n\n";
		String footer = "\nPlease report issues at https://github.com/JiyeongYun/B-SZZ\n\n";

		formatter.printHelp("BSZZ", header, options, footer, true);
	}

	private void run(String[] args) throws IOException {
		Options options = createOptions();

		if (parseOptions(options, args)) {
			if (help) {
				printHelp(options);
				return;
			}

			// Input Info
			System.out.println("\nInput Info");
			System.out.println("\tGIT URL : " + GIT_URL);
			for (String bfc : bugFixCommits)
				System.out.println("\tBFC : " + bfc);
			
			BSZZ bSZZ = new BSZZ(GIT_URL, bugFixCommits);
			bSZZ.run();

		}
	}

}
