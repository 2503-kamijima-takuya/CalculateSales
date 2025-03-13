package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SERIAL_NUMBER = "売上ファイル名が連番になっていません";
	private static final String SALES_FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String FILE_NOT_BRANCH_CODE = "の支店コードが不正です";
	private static final String SALES_AMOUNT_EXCEED_DIGIT = "合計金額が10桁を超えました";


	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// コマンドライン引数が渡されているか確認する処理
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		// ディレクトリ内すべてのファイルを取得する。
		File[] files = new File(args[0]).listFiles();
		// 「ファイル名が数字8桁で、拡張子が rcd」のファイルを格納するためのList
		ArrayList<File> rcdFiles = new ArrayList<File>();

		//files配列内の売上ファイルだけをrcdFilesリストへ格納する処理
		for(int i = 0; i < files.length; i++) {
			if(files[i].isFile() && files[i].getName().matches("^[0-9]{8}[.]rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		// 売上ファイルが連番になっているか確認する処理
		Collections.sort(rcdFiles);
		for(int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

		      //⽐較する2つのファイル名の先頭から数字の8⽂字を切り出し、int型に変換します。
			if((latter - former) != 1) {
				//2つのファイル名の数字を⽐較して、差が1ではなかったら、エラーメッセージをコンソールに表⽰します。
				System.out.println(FILE_NOT_SERIAL_NUMBER);
				return;
			}
		}

		String rcdLine;
		BufferedReader rcdBr = null;

		//各売上集計ファイルを読込み、支店の合計売上として計上する処理
		for(int i = 0; i < rcdFiles.size(); i++) {
			try {
				FileReader rcdFr = new FileReader(rcdFiles.get(i));
				rcdBr = new BufferedReader(rcdFr);
				ArrayList<String> fileContents = new ArrayList<String>();

				while((rcdLine = rcdBr.readLine()) != null) {
					fileContents.add(rcdLine);
				}

				// 売上ファイルのフォーマットを確認する処理
				if(fileContents.size() != 2) {
					System.out.println(rcdFiles.get(i).getName() + SALES_FILE_INVALID_FORMAT);
					return;
				}

				// 売上ファイルの支店コードが支店定義ファイルに該当するか確認する処理
				if (! branchNames.containsKey(fileContents.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + FILE_NOT_BRANCH_CODE);
					return;
				}

				// 売上金額が数字なのか確認する処理
				if(!fileContents.get(1).matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				String branchCode = fileContents.get(0);
				long sale = Long.parseLong(fileContents.get(1));
				Long saleAmount = branchSales.get(branchCode) + sale;

				// 売上金額の合計が10桁を超えたか確認する処理
				if(saleAmount >= 10000000000L){
					System.out.println(SALES_AMOUNT_EXCEED_DIGIT);
					return;
				}

				branchSales.put(branchCode, saleAmount);

			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				if(rcdBr != null) {
					try {
						// ファイルを閉じる
						rcdBr.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

	}


	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			// ファイルの存在を確認する処理
			if(!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");

				// ⽀店定義ファイルのフォーマットを確認する処理
				if((items.length != 2) || (!items[0].matches("^[0-9]{3}$"))){
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], 0L);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}


	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File writeFile = new File(path, fileName);
			FileWriter fw = new FileWriter(writeFile);
			bw = new BufferedWriter(fw);

			for(String key : branchNames.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}
}
