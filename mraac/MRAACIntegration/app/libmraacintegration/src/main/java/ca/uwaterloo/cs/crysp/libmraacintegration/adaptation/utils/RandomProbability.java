package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.utils;

import java.util.Random;

public class RandomProbability {
    public int [] nums;
    public float [] cumProbs;
    public float factor;
    private Random random;

    public RandomProbability (int[] nums, float[] probs) {
        this.nums = nums;
        this.cumProbs = probs;
        assert nums.length == probs.length;
        for (int i = 1; i < this.cumProbs.length; ++i) {
            this.cumProbs[i] += this.cumProbs[i - 1];
        }
        factor = this.cumProbs[this.cumProbs.length - 1];
        random = new Random();
    }

    public int nextInt() {
        float f = random.nextFloat() * factor;
        // run a binary search to find the target zone
        int low = 0, high = this.cumProbs.length;

        while (low < high) {
            // better to avoid the overflow
            int mid = low + (high - low) / 2;
            if (f > this.cumProbs[mid])
                low = mid + 1;
            else
                high = mid;
        }
        return low;
    }

    public void setSeed(long seed) {
        random = new Random(seed);
    }
}
