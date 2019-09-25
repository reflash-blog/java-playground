import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.*;
import java.nio.file.*;
import java.nio.*;
import java.io.IOException;

public class AkelarreCypher {
  
  // to encrypt encrypt DF84995582935304DF84995582935304 4BA35C086703AE2E3CEB7D824157F3BD 
  // to decrypt decrypt 50A365C8F45A9A7A7BF52675F52E9BBF 4BA35C086703AE2E3CEB7D824157F3BD
  public static void main(String[] args) throws IOException{
	  	if (args.length < 3) {
			System.out.println("Usage: AkelarreCrypter encrypt|decrypt <file> <fileout> [key]");
			return;
		}
		
		String fileIn = args[1];
		String fileOut = args[2];

		Random r = new Random();
		ArrayList<Integer> result = new ArrayList<Integer>();
		
		String action = args[0];
		int[] text = convertToIntArray(Files.readAllBytes(Paths.get(fileIn)));
		//int[] text   = AkelarreCrypter.hexStringToIntegerArray(args[1]);
		System.out.println("input:  " + AkelarreCrypter.toHexString(text));
		if (!action.equalsIgnoreCase("encrypt") && args.length < 4) {
			System.out.println("Usage: AkelarreCrypter decrypt <filein> <fileout> <key>");
			return;
		}
		
		int[] key = {r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt()};
		
		if (args.length >= 3)
			key = AkelarreCrypter.hexStringToIntegerArray(args[3]);
		
		AkelarreCrypter cipher = new AkelarreCrypter(1, key.length * 4, key);
		
		int[] header = Arrays.copyOfRange(text, 0, 224);
		text = Arrays.copyOfRange(text, 224, text.length);
		int[][] splitted = splitArray(text, 4);
		for(int i = 0; i < splitted.length; i++){
				int[] t = splitted[i];
				int[] enc = action.equalsIgnoreCase("encrypt") 
			                ? cipher.encrypt(t)
			                : cipher.decrypt(t);
				for(int j = 0; j < enc.length; j++)
					result.add(enc[j]);
		}		
		
		int[] res = new int[header.length + result.size()];
    
    for(int i = 0; i < header.length; i++)
			res[i] = header[i];
				
		for(int i = 224; i < (result.size() + 224); i++)
			if (result.get(i - 224) != null)
				res[i] = result.get(i - 224);

		System.out.println("key:     " + AkelarreCrypter.toHexString(key));
		System.out.println("output:  " + AkelarreCrypter.toHexString(res));
		Files.write(Paths.get(fileOut), convertToByteArray(res));
  }

  public static int[] convertToIntArray(byte[] input){
	int[] ret = new int[input.length/4];
	byte[] bytes;
	for (int i = 0; i < input.length/4; i++){
		bytes = new byte[4];
		if(i * 4 < input.length)
			bytes[0] = input[i*4];
		if(i * 4 + 1 < input.length)
			bytes[1] = input[i*4+1];
		if(i * 4 + 2 < input.length)
			bytes[2] = input[i*4+2];
		if(i * 4 + 3 < input.length)
			bytes[3] = input[i*4+3];
		ret[i] = bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);//input[i] & 0xff;
	}
	return ret;
  }

    public static byte[] convertToByteArray(int[] input){
	byte[] ret = new byte[input.length*4];
	for (int i = 0; i < input.length; i++){
		ret[i*4] = (byte)(input[i] >> 24);
		ret[i*4+1] = (byte)(input[i] >> 16);
		ret[i*4+2] = (byte)(input[i] >> 8);
		ret[i*4+3] = (byte)input[i];
	}
	return ret;
  }

	public static int[][] splitArray(int[] arrayToSplit, int chunkSize){
		int rest = arrayToSplit.length % chunkSize;  
		int chunks = arrayToSplit.length / chunkSize + (rest > 0 ? 1 : 0); 
		int[][] arrays = new int[chunks][];

		for(int i = 0; i < (rest > 0 ? chunks - 1 : chunks); i++)
			arrays[i] = Arrays.copyOfRange(arrayToSplit, i * chunkSize, i * chunkSize + chunkSize);

		if(rest > 0){ 
			arrays[chunks - 1] = Arrays.copyOfRange(arrayToSplit, (chunks - 1) * chunkSize, (chunks - 1) * chunkSize + rest);
			int[] copy = arrays[chunks - 1];
			arrays[chunks - 1] = new int[chunkSize];
			for(int j = 0; j < copy.length; j++)
				arrays[chunks - 1][j] = copy[j];
			for(int j = copy.length; j < chunkSize - arrays[chunks - 1].length; j++)
				arrays[chunks - 1][j] = 0;
		}

		return arrays; 
	}
}


class AkelarreCrypter {

	public static final int DEFAULT_KEY_SIZE = 64/8;
	public static final int DEFAULT_NUM_ROUNDS = 4;
	public int num_rounds;
	private static final int LAST_7_BITS = 0x0000007F;
	private KeyScheduler scheduler;
	public boolean doRotate = false;
	private int[] Z;
	private int[] D;

	
	public static int[] hexStringToIntegerArray(String hexString) {
		int[] k = new int[hexString.length() / 8];
		int ct = 0;

		for(int i = 0;i < hexString.length(); i += 8) {
			int piece = 0;
			for(int j = 0; j < 8; ++j) 
				piece |= Integer.parseInt("" + hexString.charAt(i + j), 16) << 28 - (j * 4);

			k[ct++] = piece;
		}

		return k;
	}
	

	public static void printText(int[] ints){
      Arrays.stream(ints)
          .forEach(c -> System.out.print(" " + Integer.toHexString(c).toUpperCase()));
	}
	
	public static String toHexString(int[] ints){
      	StringBuilder build = new StringBuilder();
		
      	Arrays.stream(ints)
          .forEach(c -> build.append(" " + Integer.toHexString(c).toUpperCase()));
		
		return build.toString();
	}

	private static int[] gen_random_key(int size,int mask){
		int[] key = new int[size/4];
		Random rand = new Random(42);
		for(int i=0; i<key.length; i++){
			key[i] = rand.nextInt() & (mask-1);
		}

		System.out.println("Key: " + Integer.toHexString(key[0]));

		return key;
	}

	public AkelarreCrypter(int mask){
		this(DEFAULT_NUM_ROUNDS,4,gen_random_key(4,mask));
	}

	public AkelarreCrypter(int[] key){
		this(DEFAULT_NUM_ROUNDS,DEFAULT_KEY_SIZE,key);
	}

	public AkelarreCrypter(int num_rounds, int key_size, int[] key){
		this.num_rounds = num_rounds;
		scheduler = new KeyScheduler(num_rounds,key_size);
		Z = scheduler.schedule(key);
		D = scheduler.createDecryptionSubkeys(Z);
	}

	public int[] encrypt(int[] plaintext){
		if (plaintext.length != 4)
			throw new IllegalArgumentException();
		return encrypt(plaintext.clone(), Z);
	}

	public int[] decrypt( int[] cipherText ){
		return encrypt(cipherText.clone(), D);
	}

	public int[] encrypt(int[] a, int[] k){
		a[0] += k[0];
		a[1] ^= k[1];
		a[2] ^= k[2];
		a[3] += k[3];

		int P1, P2, t0, t1;
		
		for( int r=0; r < num_rounds; r++){

			if( doRotate )
				rotl128(a,  k[13*r+4] & LAST_7_BITS);

			P1 = a[0] ^ a[2]; 
			P2 = a[1] ^ a[3]; 

			t1 = rotl31(P1,P2&0x1f);
			t1+= k[13*r+5];
			t1 = rotl1(t1,(P1>>>5)&0x1f);
			t1+= k[13*r+6];
			t1 = rotl31(t1,(P1>>>10) &0x1f);
			t1+= k[13*r+7];
			t1 = rotl1(t1,(P1>>>15) &0x1f);
			t1+= k[13*r+8];
			t1 = rotl31(t1,(P1>>>20)&0xf);
			t1+= k[13*r+9];
			t1 = rotl1(t1,(P1>>>24)&0xf);
			t1+= k[13*r+10];

			t0 = rotl1(t1,P1&0x1f);
			t0+= k[13*r+11];
			t0 = rotl31(t0,(P1>>>5)&0x1f);
			t0+= k[13*r+12];
			t0 = rotl1(t0,(P1>>>10)&0x1f);
			t0+= k[13*r+13];
			t0 = rotl31(t0,(P1>>>15)&0x1f);
			t0+= k[13*r+14];
			t0 = rotl1(t0,(P1>>>20)&0xf);
			t0+= k[13*r+15];
			t0 = rotl31(t0,(P1>>>24)&0xf);
			t0+= k[13*r+16];

			a[0] ^=t1;
			a[2] ^=t1;
			a[1] ^=t0;
			a[3] ^=t0;

		}


		if( doRotate )
			rotl128(a,  k[13*num_rounds+4] & LAST_7_BITS);

		a[0] = a[0]+k[13*num_rounds+5];
		a[1] = a[1]^k[13*num_rounds+6];
		a[2] = a[2]^k[13*num_rounds+7];
		a[3] = a[3]+k[13*num_rounds+8];

		return a;
	}

	public static int rotl31( int x, int y )
	{
		int bit = x & 0x1;
		x &= 0xfffffffe;
		return ((x<<y) | (x>>>(31-y)))|bit;
	}

	public static int rotl1( int x, int y )
	{
		int bit = x & 0x80000000;
		x &= 0x7fffffff;
		return ((x<<y) | (x>>>(31-y)))|bit;
	}

	public static void rotl128(int[] input, int amount){
		int shiftAmount = amount % 128;
		int overflow = 0;

		while( amount > 0 ){
			shiftAmount = amount;
			if( shiftAmount > 31 )
				shiftAmount = 31;

			overflow = (input[0] >>> (32-shiftAmount));
			for( int i=0; i < input.length-1; i++)
				input[i] = (input[i]<<shiftAmount) | (input[i+1] >>> (32-shiftAmount));

			input[input.length-1] = (input[input.length-1]<<shiftAmount) | overflow;

			amount -= shiftAmount;
		}
    }

	public static String getBytes(int input){
		StringBuilder build = new StringBuilder(32);
		build.append(input<0?"1":"0");
		for(int i=30; i>=0; i--){
			boolean match = (input & (1 << i)) > 0;
			build.append(match?"1":"0");
		}

		return build.toString();
	}

	public static int[] bytesToInt(String input){
		int[] ints = new int[input.length() / 32];
		for(int i=0;i<ints.length;i++){
			ints[i] = 0;
			for(int j=0;j<32;j++)
				ints[i] |= ((input.charAt(32*i+j) == '1'?1:0) << (31-j));
		}
		return ints;
	}
}

class KeyScheduler {
	private final int NUM_ROUNDS;
	public final int KEY_SIZE; 
	private final int A0 = 0xA49ED284;
	private final int A1 = 0x735203DE;

	public KeyScheduler(int num_rounds,int key_size){
		NUM_ROUNDS = num_rounds;
		KEY_SIZE = key_size;
	}

	public int[] schedule(int[] key){
		int[] K = new int[13*NUM_ROUNDS+9];

		if( key.length != KEY_SIZE/4){
			throw new IllegalArgumentException("Key provided of improper length.");
		}

		int[] s = new int[key.length*2];
		int[] u = new int[key.length*2];
		int[] v = new int[key.length*2];
		for(int i=0;i<s.length;i+=2){
			s[i]     = key[i/2] >>> 16;
			s[i+1]   = key[i/2] & 0xFFFF;

			u[i]     = (s[i]*s[i]+A0);
			v[i]     = (s[i]*s[i]+A1);
			u[i+1]   = (s[i+1]*s[i+1]+A0);
			v[i+1]   = (s[i+1]*s[i+1]+A1);
		}


		for(int i=0;i<13*NUM_ROUNDS+9;i++){
			int index = i % s.length;
			int um = (u[index] >> 8) & 0xFFFF;
			int vm = (v[index] >> 8) & 0xFFFF;

			K[i] = ((u[index] << 24) & 0xFF000000) | ((u[index] >> 8) & 0xFF0000)  | ((v[index] << 8) & 0xFF00) | ((v[index] >> 24) & 0xFF);


			u[index] = um*um+A0;
			v[index] = vm*vm+A1;
		}
		return K;
	}

	public static int neg(int A){
		int x = A&0x7f;
		int y = (-(x % 128));
		return (A & 0xFFFFFF80) | (y&0x7f);
	}


	public int[] createDecryptionSubkeys(int[] Z){
		int[] D = new int[13*NUM_ROUNDS+9];
		D[0] = -Z[13*NUM_ROUNDS+5];
		D[1] =  Z[13*NUM_ROUNDS+6];
		D[2] =  Z[13*NUM_ROUNDS+7];
		D[3] = -Z[13*NUM_ROUNDS+8];

		for(int r=0;r<=NUM_ROUNDS-1;r++){
			D[13*r+4] = neg(Z[13*(NUM_ROUNDS-r)+4]);
			for( int j=5;j<=16;j++ ){
				D[13*r+j] = Z[13*(NUM_ROUNDS-r-1)+j];
			}
		}

		D[13*NUM_ROUNDS+4] = neg(Z[4]);

		D[13*NUM_ROUNDS+5] = -Z[0];
		D[13*NUM_ROUNDS+6] =  Z[1];
		D[13*NUM_ROUNDS+7] =  Z[2];
		D[13*NUM_ROUNDS+8] = -Z[3];
		return D;
	}
}