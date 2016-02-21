package com.clust4j;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.math3.util.FastMath;

import com.clust4j.algo.preprocess.FeatureNormalization;

/**
 * A set of global config values used in multiple classes. Some values may
 * be set to the user's preference, while others are final.
 * 
 * @author Taylor G Smith
 */
public class GlobalState {
	/** The default random state */
	public final static Random DEFAULT_RANDOM_STATE = new Random(999);
	public final static int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 5;
	
	
	
	/**
	 * Holds static mathematical values
	 * @author Taylor G Smith
	 */
	public static final class Mathematics {
		/** Double.MIN_VALUE is not negative; this is */
		public final static double SIGNED_MIN = Double.NEGATIVE_INFINITY;
		public final static double MAX = Double.POSITIVE_INFINITY;
		public final static double TINY = 2.2250738585072014e-308;
		public final static double EPS = 2.2204460492503131e-16;
		
		/*===== Gamma function assistants =====*/
		public final static double LOG_PI  = FastMath.log(Math.PI);
		public final static double LOG_2PI = FastMath.log(2 * Math.PI);
		public final static double ROOT_2PI= FastMath.sqrt(2 * Math.PI);
		/** Euler's Gamma constant */
		public final static double GAMMA = 0.577215664901532860606512090;
		public final static double HALF_LOG2_PI = 0.91893853320467274178032973640562;
		final static double[] GAMMA_BOUNDS = new double[]{0.001, 12.0};
		final static double HIGH_BOUND = 171.624;
		
		/** numerator coefficients for approximation over the interval (1,2) */
		private final static double[] p = new double[]{
		   -1.71618513886549492533811E+0,
            2.47656508055759199108314E+1,
           -3.79804256470945635097577E+2,
            6.29331155312818442661052E+2,
            8.66966202790413211295064E+2,
           -3.14512729688483675254357E+4,
           -3.61444134186911729807069E+4,
            6.64561438202405440627855E+4
		};
		
		/** denominator coefficients for approximation over the interval (1,2) */
		private final static double[] q = new double[]{
		   -3.08402300119738975254353E+1,
            3.15350626979604161529144E+2,
           -1.01515636749021914166146E+3,
           -3.10777167157231109440444E+3,
            2.25381184209801510330112E+4,
            4.75584627752788110767815E+3,
           -1.34659959864969306392456E+5,
           -1.15132259675553483497211E+5
		};
		
		/**
	     * Abramowitz and Stegun 6.1.41
	     * Asymptotic series should be good to at least 11 or 12 figures
	     * For error analysis, see Whittiker and Watson
	     * A Course in Modern Analysis (1927), page 252
	     */
		private final static double[] c = new double[]{
			 1.0/12.0,
	        -1.0/360.0,
	         1.0/1260.0,
	        -1.0/1680.0,
	         1.0/1188.0,
	        -691.0/360360.0,
	         1.0/156.0,
	        -3617.0/122400.0
		};
		
		// Any assertion failures will cause exception to be thrown right away
		static {
			// These should never change
			assert GAMMA_BOUNDS.length == 2;
			assert p.length == 8;
			assert p.length == q.length;
			assert c.length == p.length;
		}
		
		/**
		 * Adapted from sklearn_gamma, which was in turn adapted from
		 * John D. Cook's public domain version of lgamma, from
		 * http://www.johndcook.com/stand_alone_code.html
		 * @param x
		 * @return
		 */
		public static double gamma(double x) {
			if(x <= 0)
				throw new IllegalArgumentException("x must exceed 0");
			
			// Check if in first boundary
			int boundaryIdx = 0;
			if(x < GAMMA_BOUNDS[boundaryIdx++])
				return 1.0 / (x * (1.0 + GAMMA * x));
			
			if(x < GAMMA_BOUNDS[boundaryIdx++]) {
				double den = 1.0, num = 0.0, res, z, y = x;
				int i, n = 0;
				boolean lt1 = y < 1.0;
				
				if(lt1)
					y += 1.0;
				else {
					n = ((int)y) - 1;
					y -= n;
				}
				
				z = y - 1;
				for(i = 0; i < p.length; i++) {
					num = (num + p[i]) * z;
					den = den * z + q[i];
				}
				
				res = num/den + 1.0;
				
				// Correction if arg was not initially in (1,2)
				if(lt1)
					res /= (y - 1.0);
				else {
					for(i = 0; i < n; i++, y++)
						res *= y;
				}
				
				return res;
			}
			
			if(x > HIGH_BOUND)
				return Double.POSITIVE_INFINITY;
			
			return FastMath.exp(lgamma(x));
		}
		
		public static double lgamma(double x) {
			if(x <= 0)
				throw new IllegalArgumentException("x must exceed 0");
			
			double z, sum;
			int i;
			
			if(x < GAMMA_BOUNDS[1])
				return FastMath.log(FastMath.abs(gamma(x)));
			
			z = 1.0/ (x * x);
			sum = c[7];
			for(i = 6; i >= 0; i--) {
				sum *= z;
				sum += c[i];
			}
			
			return (x - 0.5) * FastMath.log(x) - x + HALF_LOG2_PI + sum / x;
		}
	}
	
	
	
	/**
	 * A class to hold configurations for parallelism
	 * @author Taylor G Smith
	 */
	public final static class ParallelismConf {
		/**
		 * The minimum number of required cores to efficiently
		 * allow parallel operations.
		 */
		public static final int MIN_PARALLEL_CORES_REQUIRED = 8;
		
		/**
		 * The number of available cores on the machine. Used for determining
		 * whether or not to use parallelism & how large parallel chunks should be. */
		public static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
		
		/**
		 * Whether parallelization is recommended for this machine.
		 * Default value is true if availableProcessors is at least 8.
		 */
		public static final boolean PARALLELISM_RECOMMENDED = NUM_CORES >= MIN_PARALLEL_CORES_REQUIRED;
		
		/** If true and the size of the vector exceeds {@value #MAX_SERIAL_VECTOR_LEN}, 
		 *  auto schedules parallel job for applicable operations. This can slow
		 *  things down on machines with a lower core count, but speed them up
		 *  on machines with a higher core count. More heap space may be required. 
		 *  Defaults to {@link #PARALLELISM_RECOMMENDED}
		 */
		public static boolean ALLOW_AUTO_PARALLELISM = PARALLELISM_RECOMMENDED;
		
		/**
		 * If true, will try to force parallelism whenever possible. On a machine
		 * with fewer cores, can dramatically slow down some processes. Use with
		 * caution!
		 */
		public static boolean FORCE_PARALLELISM_WHERE_POSSIBLE = false;
		
		/**
		 * The global ForkJoin thread pool for parallel recursive tasks. */
		final static public ForkJoinPool FJ_THREADPOOL = new ForkJoinPool();
		
		/**
		 * The max length a vector may be before defaulting to a parallel process, if applicable */
		static public int MAX_SERIAL_VECTOR_LEN = 10_000_000;

		/** 
		 * The max length a parallel-processed chunk may be */
		public static int MAX_PARALLEL_CHUNK_SIZE = MAX_SERIAL_VECTOR_LEN / NUM_CORES; //2_500_000;
	}
	
	
	
	
	/**
	 * A class to hold configurations for FeatureNormalization
	 * @author Taylor G Smith
	 */
	public final static class FeatureNormalizationConf {
		/** The lower bound for the {@link FeatureNormalization#MIN_MAX_SCALE} scaling range. */
		public static int MIN_MAX_SCALER_RANGE_MIN = 0;
		/** The upper bound for the {@link FeatureNormalization#MIN_MAX_SCALE} scaling range. */
		public static int MIN_MAX_SCALER_RANGE_MAX = 1;
	}
}
