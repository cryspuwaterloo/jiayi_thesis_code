# metrics: used for model evaluation

import numpy as np
from sklearn.metrics import roc_auc_score
from sklearn.metrics import roc_curve
from scipy.optimize import brentq
from scipy.interpolate import interp1d


def eer(y, y_score):
    """
    from https://yangcha.github.io/EER-ROC/
    """
    fpr, tpr, thresholds = roc_curve(y, y_score, pos_label=1)
    result = brentq(lambda x : 1. - x - interp1d(fpr, tpr)(x), 0., 1.)
    thresh = interp1d(fpr, thresholds)(result)
    return result, thresh

def multiclass_eer(y_true, y_pred):
    n_classes = y_pred.shape[1]
    return [
        eer((np.array(y_true) == label).astype(int),
            y_pred[:, label])
        for label in range(n_classes)
    ]

def multiclass_auc(y_true, y_pred):
    n_classes = y_pred.shape[1]
    return [
        roc_auc_score((np.array(y_true) == label).astype(int),
            y_pred[:, label])
        for label in range(n_classes)
    ]