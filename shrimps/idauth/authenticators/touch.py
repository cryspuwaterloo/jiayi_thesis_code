"""
Touch based authenticator is based on Touchalytics:
http://www.mariofrank.net/touchalytics/

We modify the original code to fit into our framework
"""


from idauth.authenticators.base import Authenticator

import math
import cmath
import imblearn
import numpy as np
from idauth.utils.metrics import multiclass_auc, multiclass_eer
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score, classification_report
from sklearn.preprocessing import MinMaxScaler


class TouchPoint:
    """Data for a single touch point on the screen"""
    def __init__(self, user_id, session_id, swipe_id, tstamp, x, y, pressure,
                 area, orientation, activity):
        self.user_id = user_id
        self.session_id = session_id
        self.swipe_id = swipe_id
        self.tstamp = tstamp
        self.x = x
        self.y = y
        self.pressure = pressure
        self.area = area
        self.orientation = orientation
        self.activity = activity

    def __str__(self):
        return "user: " + self.user_id + ", session_id: " + str(self.session_id) + \
            ", swipe_id: " + str(self.swipe_id) +\
            ", time: " + str(self.tstamp) + ", x: " + str(self.x) + ", y: " + str(self.y) + \
              ", pressure: " + str(self.pressure) + ", area: " + str(self.area) + \
              ", orientation: " + str(self.orientation) + ", activity: " + str(self.activity)



class TouchAuth(Authenticator):

    FEATURE_LIST = [
        'user_id', 'session_id', 'activity', 'swipe_id', 'direction_flag',
        'interstroke_time', 'stroke_duration', 'start_x', 'start_y',
        'stop_x', 'stop_y', 'direct_end_to_end_distance',
        'mean_resultant_length', 'direction_of_end_to_end_line',
        'pairwise_velocity_20p_perc', 'pairwise_velocity_50p_perc',
        'pairwise_velocity_80p_perc', 'pairwise_acc_20p_perc',
        'pairwise_acc_50p_perc', 'pairwise_acc_80p_perc',
        'median_velocity_last_3_pts', 'largest_dev_end_to_end_line',
        'dev_end_to_end_line_20p_perc', 'dev_end_to_end_line_50p_perc',
        'dev_end_to_end_line_80p_perc', 'avg_direction',
        'length_of_trajectory', 'ratio_end_to_end_dist_length_of_traj',
        'avg_velocity', 'median_acc_first_5_pts', 'mid_stroke_pressure',
        'mid_stroke_area', 'phone_orientation'
    ]

    @staticmethod
    def raw_to_touchpoints(raw_data: list, user="unknown", session="null",
                           sel_activities=None):
        tps = []
        for i in raw_data:
            if sel_activities is not None:
                if i[7] not in sel_activities:
                    continue
            tps.append(TouchPoint(user, session, i[0],
                                  i[1], i[2], i[3],
                                  i[4], i[5], i[6],
                                  i[7]))
        return tps

    @staticmethod
    def tps_to_feature_vector(tps, last_swipe_time):
        """
        Returns a feature vector generated from raw_data,
        which is a tuple (list of TouchPoint, last swipe time).
        """
        fv = [0.0] * len(TouchAuth.FEATURE_LIST)

        fv[0] = tps[0].user_id
        fv[1] = tps[0].session_id
        fv[2] = tps[0].activity
        fv[3] = tps[0].swipe_id

        fv[5] = tps[0].tstamp - last_swipe_time  # interstroke_time
        fv[6] = tps[-1].tstamp - tps[0].tstamp  # stroke_duration
        fv[7] = tps[0].x
        fv[8] = tps[0].y
        fv[9] = tps[-1].x
        fv[10] = tps[-1].y

        # direct end-to-end distance
        fv[11] = math.sqrt(
            math.pow(fv[9] - fv[7], 2) + math.pow(fv[10] - fv[6], 2))

        x_disp, y_disp, t_disp = list(), list(), list()
        for i in range(1, len(tps)):
            x_disp.append(tps[i].x - tps[i - 1].x)
            y_disp.append(tps[i].y - tps[i - 1].y)
            t_disp.append(tps[i].tstamp - tps[i - 1].tstamp)

        pairw_angle = []
        for i in range(0, len(x_disp)):
            pairw_angle.append(math.atan2(y_disp[i], x_disp[i]))

        fv[12] = circ_r(pairw_angle)  # 8 Mean Resultant Length

        # Direction Flag (up, down, left, right are 0,1,2,3)
        fv[4] = 'down'  # down is default
        x_diff = fv[9] - fv[7]
        y_diff = fv[10] - fv[8]
        if math.fabs(x_diff) > math.fabs(y_diff):
            if x_diff < 0:
                fv[4] = 'left'
            else:
                fv[4] = 'right'
        else:
            if y_diff < 0:
                fv[4] = 'up'

        fv[13] = math.atan2(fv[10] - fv[8],
                            fv[9] - fv[7])  # direction of end-to-end line

        pairw_dist = []
        for i in range(0, len(x_disp)):
            pairw_dist.append(
                math.sqrt(math.pow(x_disp[i], 2) + math.pow(y_disp[i], 2)))

        pairw_v = []
        for i in range(0, len(pairw_dist)):
            if t_disp[i] == 0:
                pairw_v.append(0)
            else:
                pairw_v.append(pairw_dist[i] / t_disp[i])
        max_v = max(
            pairw_v)  # replace 0 v with max(v) as that is more appropriate
        for i in range(0, len(pairw_v)):
            if pairw_v[i] == 0:
                pairw_v[i] = max_v
        pairw_a = []
        for i in range(1, len(pairw_v)):
            pairw_a.append(pairw_v[i] - pairw_v[i - 1])
        for i in range(0, len(pairw_a)):
            if t_disp[i] == 0:
                pairw_a[i] = 0  # replace with max acceleration-done below
            else:
                pairw_a[i] = pairw_a[i] / t_disp[i]

        max_a = max(pairw_a)
        for i in range(0, len(pairw_a)):
            if pairw_a[i] == 0:
                pairw_a[i] = max_a

        pairw_v3 = pairw_v[-4:]
        pairw_a6 = pairw_a[0:6]
        pairw_v.sort()
        pairw_a.sort()
        pairw_v3.sort()
        pairw_a6.sort()

        fv[14] = percentile(pairw_v, 0.20)  # 20% percentile of velocity
        fv[15] = percentile(pairw_v, 0.50)  # 50% percentile of velocity
        fv[16] = percentile(pairw_v, 0.80)  # 80% percentile of velocity
        fv[17] = percentile(pairw_a, 0.20)  # 20% percentile of acceleration
        fv[18] = percentile(pairw_a, 0.50)  # 50% percentile of acceleration
        fv[19] = percentile(pairw_a, 0.80)  # 80% percentile of acceleration

        fv[20] = percentile(pairw_v3, 0.50)  # median velocity at last 3 points

        # 26 Largest deviation from end-end line
        xvek, yvek = list(), list()
        for i in range(0, len(tps)):
            xvek.append(tps[i].x - fv[7])
            yvek.append(tps[i].y - fv[8])

        pervek = [yvek[-1], xvek[-1] * -1, 0]
        temp = math.sqrt(pervek[0] * pervek[0] + pervek[1] * pervek[1])
        if temp == 0:
            for i in range(0, len(pervek)):
                pervek[i] = 0
        else:
            for i in range(0, len(pervek)):
                pervek[i] = pervek[i] / temp

        proj_perp_straight = []
        abs_proj = []
        for i in range(0, len(xvek)):
            proj_perp_straight.append(xvek[i] * pervek[0] + yvek[i] * pervek[1])
            abs_proj.append(math.fabs(proj_perp_straight[i]))
        fv[21] = max(abs_proj)
        fv[22] = percentile(abs_proj, 0.20)  # 20% deviation from end-end line
        fv[23] = percentile(abs_proj, 0.50)  # 50% deviation from end-end line
        fv[24] = percentile(abs_proj, 0.80)  # 80% deviation from end-end line

        fv[25] = circ_mean(pairw_angle)  # average direction of ensemble pairs

        fv[26] = 0  # length of trajectory
        for pd in pairw_dist:
            fv[26] += pd

        if fv[26] == 0:
            fv[27] = 0  # Ratio of direct distance and trajectory length
        else:
            fv[27] = fv[11] / fv[26]

        if fv[6] == 0:  # fv[6] is stroke duration; fv[26] length of traj.
            fv[28] = 0  # Average Velocity
        else:
            fv[28] = fv[26] / fv[6]

        fv[29] = percentile(pairw_a6,
                            0.50)  # Median acceleration at first 5 points

        fv[30] = tps[
            int(len(tps) / 2)].pressure  # pressure in the middle of stroke
        fv[31] = tps[int(len(tps) / 2)].area  # area in the middle of stroke

        fv[32] = tps[0].orientation

        return fv

    def extract_features(self, raw_data, sel_activities=None):
        """
        extract features from raw data
        For touch-based authenticator, we need to get touch points first
        """
        tps = TouchAuth.raw_to_touchpoints(raw_data, sel_activities)
        # print(len(tps))

        # filter out unqualified touch events
        if len(tps) < 6:
            print("too short, discarded")
            return []

        i = 0
        swipe_id = tps[0].swipe_id
        result = []
        while i < len(tps):
            start_idx = i
            while swipe_id == tps[i].swipe_id:
                i += 1
                if i == len(tps):
                    break

            end_idx = i - 1
            if i == len(tps):
                swipe_id = tps[i - 1].swipe_id
            else:
                swipe_id = tps[i].swipe_id

            last_swipe_end_time = tps[
                0].tstamp  # if last swipe DNE, set to current rather than 0
            if start_idx != 0:
                last_swipe_end_time = tps[start_idx - 1].tstamp
            if end_idx - start_idx > 5:
                # Cleanse data
                fv = TouchAuth.tps_to_feature_vector(
                    tps[start_idx:end_idx], last_swipe_end_time)
                if fv[5] <= 600000 and fv[
                    5] > 0:  # interstroke_time must be above 0 and less than 10 minutes
                    if fv[6] <= 60000 and fv[
                        6] > 0:  # stroke_duration must be above 0 and less than 1 minute
                        if fv[3] > -1:  # swipe_id should be greater than -1
                            result.append(fv[5:-1])

        return result

    def train_model(self, train_data, test_data=None):
        """
        train_data: list of (label, features)
        test_data: list of (label, features)
        """
        y_train, X_train = zip(*train_data)
        X_train_new = self.scaler.fit_transform(X_train)
        resamp = imblearn.over_sampling.SMOTE(sampling_strategy='all')
        # dosamp = imblearn.under_sampling.CondensedNearestNeighbour()
        X_final, y_final = resamp.fit_resample(X_train_new, y_train)
        # print(Counter(y_train))
        # self.clf.fit(X_train_new, y_train)
        self.clf.fit(X_final, y_final)
        if test_data is not None:
            y_test, X_test = zip(*test_data)
            X_test_new = self.scaler.transform(X_test)
            y_predict = self.clf.predict(X_test_new)
            print(classification_report(y_test, y_predict))


    def train_model_with_eval(self, train_set, test_set):
        y_train, X_train = train_set
        X_train_new = self.scaler.fit_transform(X_train)
        resamp = imblearn.over_sampling.SMOTE(sampling_strategy='all')
        # dosamp = imblearn.under_sampling.CondensedNearestNeighbour()
        X_final, y_final = resamp.fit_resample(X_train_new, y_train)
        # print(Counter(y_train))
        # self.clf.fit(X_train_new, y_train)
        self.clf.fit(X_final, y_final)

        test_label, X_test = test_set
        X_test_new = self.scaler.transform(X_test)
        pred = self.clf.predict_proba(X_test_new)
        report = dict()
        # metrics: per-class auc, eer, precision, recall, f1-score, overall acc
        converted_y_pred = np.argmax(pred, axis=1)
        converted_y_true = np.array(test_label)
        report['precision'] = precision_score(converted_y_true,
                                              converted_y_pred,
                                              average=None)

        report['recall'] = recall_score(converted_y_true,
                                        converted_y_pred,
                                        average=None)

        report['f1'] = f1_score(converted_y_true,
                                converted_y_pred,
                                average=None)

        report['accuracy'] = accuracy_score(converted_y_true, converted_y_pred)
        report['auc'] = multiclass_auc(converted_y_true, pred)
        eer, thres = zip(*multiclass_eer(converted_y_true, pred))
        report['eer'] = eer
        report['report'] = classification_report(converted_y_true,
                                                 converted_y_pred)
        return report

    def authenticate(self, features):
        """
        features is a list of feature vectors
        """
        X_train_new = self.scaler.transform(features)
        y_predict = self.clf.predict_proba(X_train_new)
        return y_predict

    def __init__(self, clf=None, scaler=None):
        self.clf = RandomForestClassifier() if clf is None else clf
        self.scaler = MinMaxScaler((0, 3)) if scaler is None else scaler


#Translated from www.kyb.mpg.de/~berens/circStat.html
def circ_r(x):
    r = cmath.exp(1j*x[0])
    for i in range(1, len(x)):
        r += cmath.exp(1j*x[i])
    return abs(r)/len(x)

#Translated from www.kyb.mpg.de/~berens/circStat.html
def circ_mean(x):
    r = cmath.exp(1j*x[0])
    for i in range(1, len(x)):
        r += cmath.exp(1j*x[i])
    return math.atan2(r.imag, r.real)

def percentile(N, percent, key=lambda x:x):
    """
    Find the percentile of a list of values.

    @parameter N - is a list of values. Note N MUST BE already sorted.
    @parameter percent - a float value from 0.0 to 1.0.
    @parameter key - optional key function to compute value from each element of N.

    @return - the percentile of the values
    """
    if not N:
        return None
    k = (len(N)-1) * percent
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return key(N[int(k)])
    d0 = key(N[int(f)]) * (c-k)
    d1 = key(N[int(c)]) * (k-f)
    return d0+d1
