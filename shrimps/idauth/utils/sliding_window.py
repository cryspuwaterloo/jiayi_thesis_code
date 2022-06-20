import numpy as np

class SlidingWindow:
    RESULT_DATA_INSUFFICIENT = -1
    RESULT_CLASS_INSIGNIFICANT = -2

    def add(self, data):
        self.windows.append(data)
        if len(self.windows) > self.max_history:
            self.windows = self.windows[-self.max_history:]

        unique, counts = np.unique(self.windows[-self.window_size:],
                                   return_counts=True)
        index = np.argmax(counts)
        top = unique[index]
        top_n = counts[index]

        if len(self.windows) < self.threshold:
            return self.RESULT_DATA_INSUFFICIENT
        elif len(self.windows) < self.window_size:
            if self.early_result and top_n >= self.threshold:
                return top
            else:
                return self.RESULT_DATA_INSUFFICIENT
        elif top_n >= self.threshold:
            return top
        else:
            return self.RESULT_CLASS_INSIGNIFICANT

    def reset(self):
        self.windows = []

    def __init__(self, window_size, threshold, max_history=None,
                 early_result=True):
        self.window_size = window_size
        self.max_history = max_history if max_history is not None \
            else window_size * 2
        self.threshold = threshold
        self.windows = []
        self.early_result = early_result
