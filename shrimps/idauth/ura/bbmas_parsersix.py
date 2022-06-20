import math
import os
import random
import warnings

import pandas as pd
import numpy as np


class BBMASProvider:
    ACTIVITY_TYPING = 0
    ACTIVITY_CORRIDOR_WALKING = 1
    ACTIVITY_STAIR_UP = 2
    ACTIVITY_STAIR_DOWN = 3

    SUPPORTED_ACTIVITIES = [
        ACTIVITY_TYPING,
        ACTIVITY_CORRIDOR_WALKING,
        ACTIVITY_STAIR_UP,
        ACTIVITY_STAIR_DOWN
    ]

    SUPPORTED_SENSORS = [
        'acc', 'gyro', 'touch'
    ]

    def __get_user_session_dir(self, user, session):
        return os.path.join(self.path, str(user),
                            str(user) + "_session_" + str(session))

    def __get_check_points_dir(self, user, event):
        result = [i for i in
                  os.listdir(os.path.join(self.path, user))
                  if (os.path.isdir(os.path.join(self.path, user)) and event in i)]
        return os.path.join(self.path, user, result[0])

    def get_task_desc(self, task_id):
        if task_id in [1]:
            return self.ACTIVITY_TYPING
        elif task_id in [2, 4, 6]:
            return self.ACTIVITY_CORRIDOR_WALKING
        elif task_id in [3]:
            return self.ACTIVITY_STAIR_UP
        elif task_id in [5]:
            return self.ACTIVITY_STAIR_DOWN
        else:
            raise ValueError("Invalid Activity ID received: " + str(task_id))

    def get_activityID_to_task_desc(self, user, session,
                                    filename="Activity.csv"):
        """
        Maps activityID to activity description
        """
        activity = dict()
        for i in range(1, 7):
            activity[i] = self.get_task_desc(i)
        return activity

    def get_user_sessions_with_activity(self, user, activity):
        return [i for i, session in self.data[user].items()
                if 'acc' in session.keys() and session['acc'] is not None
                and 'activity' in session['acc'].keys()
                and session['acc']['activity'].iloc[0] == activity]

    def get_user_sessions_with_touch_activity(self, user, activity):
        # return [i for i, session in self.data[user].items()
        #         if 'touch' in session.keys() and session['touch'] is not None
        #         and 'activity' in session['touch'].keys()
        #         and session['touch']['activity'].iloc[0] == activity]
        list_sess = [100, 101, 102, 103, 104]
        return list_sess

    def get_sessions_for_unloaded_user(self, user, activities):
        sessions = self.__load_all_sessions_of_user(user, self.path)
        result = []
        for session in sessions:
            task_desc = self.get_activityID_to_task_desc(user, session)
            file_complete_path = os.path.join(
                self.__get_user_session_dir(user, session), "Accelerometer.csv")

            count = 0
            with open(file_complete_path, 'r') as fp:
                for line in fp:
                    if count == 0:
                        count += 1
                        continue
                line = line.split(',')
                if len(line) < 3:
                    print(user, session, "lacks acc data.")
                    continue
                if line[-1] != 'activity\n':
                    if task_desc[int(line[-1])] in activities:
                        result.append(session)
        return result

    def __load_all_users(self, path=None):
        """
        Obtain a list of all users under the directory
        """
        to_open = path if path is not None else self.path

        user_list = [i for i in os.listdir(to_open)
                     if os.path.isdir(os.path.join(to_open, i))]  # C:/UWaterloo/4A/URA - 2/small_bbmas/dataset\#1
        user_list.sort(key=lambda user: int(user))
        return user_list

    def __load_all_sessions_of_user(self, user, path=None):
        to_open = path if path is not None else self.path
        result = [int(i.split("_")[-1]) for i in
                  os.listdir(os.path.join(to_open, user))
                  if os.path.isdir(os.path.join(to_open, user, i))
                  and i[0] != "."]
        return sorted(result)

    def get_all_users(self):
        """
        Return a list of all users
        """
        return self.users

    def load_touch_data(self, user, session, filename="TouchEvent.csv"):
        """
        Load touch data for a single user-session
        """
        file_complete_path = os.path.join(
            self.__get_user_session_dir(user, session),
            filename)

        if not file_complete_path or not os.path.exists(file_complete_path):
            return None

        with open(file_complete_path, 'r') as fp:
            result = []
            count = 0
            for line in fp:
                if count == 0:
                    count += 1
                    continue
                l = [float(x) for x in line.split(',')]
                item = {
                    'swipe_id': int(l[8]),
                    'time': int(l[1] * 1000),
                    'x': l[2],
                    'y': l[3],
                    'pressure': l[4],
                    'area': math.pi * l[5] * l[6],
                    'orientation': l[7],
                    'activity': int(self.get_task_desc(l[-1])),
                }
                result.append(item)
        return pd.DataFrame(result)

    def load_motion_data(self, user, session, filename="Accelerometer.csv"):
        """
        Load acc/gyro for a single user-session
        """
        file_complete_path = os.path.join(
            self.__get_user_session_dir(user, session),
            filename)

        if not file_complete_path or not os.path.exists(file_complete_path):
            return None

        with open(file_complete_path, 'r') as fp:
            result = []
            count = 0
            for line in fp:
                if count == 0:
                    count += 1
                    continue
                l = [float(x) for x in line.split(',')]
                item = {
                    'time': int(l[1] * 1000),
                    'x': l[2],
                    'y': l[3],
                    'z': l[4],
                    'activity': int(self.get_task_desc(l[-1]))
                }
                result.append(item)
        return pd.DataFrame(result)

    def load_keystroke_data(self, user, session, filename="Keyboard.csv"):
        """
        Load keystroke for a single user-session
        """
        file_complete_path = os.path.join(
            self.__get_user_session_dir(user, session),
            filename)

        if not file_complete_path or not os.path.exists(file_complete_path):
            return None

        with open(file_complete_path, 'r') as fp:
            result = []
            count = 0
            for line in fp:
                if count == 0:
                    count += 1
                    continue
                l = [float(x) for x in line.split(',')]
                item = {
                    'time': int(l[1] * 1000),
                    'key': l[2],
                    'activity': int(self.get_task_desc(l[-1]))
                }
                result.append(item)
        return pd.DataFrame(result)

    def load_single_user(self, user):
        assert user in self.users
        if user in self.data:
            return
        result = dict()

        sessions = self.__load_all_sessions_of_user(user, self.path)
        for session in sessions:
            # print(user, session)
            item = {
                'acc': self.load_motion_data(user, session, "Accelerometer.csv"),
                'gyro': self.load_motion_data(user, session, "Gyroscope.csv"),
                'touch': self.load_touch_data(user, session, "TouchEvent.csv"),
                'keystroke': self.load_keystroke_data(user, session, "Keyboard.csv")
            }
            result[session] = item

        self.data[user] = result

    def load(self, sel_users=None):
        """
        load the data field of bbmas, sel_users is a list of users wish to load
        """
        user_pool = self.users if sel_users is None else sel_users
        for user in user_pool:
            self.load_single_user(user)

    def gen_train_test_sessions(self, user, test_num, activities=None,
                                random_state=None):
        """
        return training sessions and test session numbers for a user
        :param user: target user id
        :param test_num: numbers of test sessions
        :param activities: selected activities
        :param random_state: None (no random), or random seed
        :return: train session number list and test sessions number list
        """
        train_sessions = {}
        test_sessions = {}
        sel_activities = activities if activities is not None \
            else self.SUPPORTED_ACTIVITIES

        if random_state is not None:
            np.random.seed(random_state)

        for activity in sel_activities:
            sess = self.get_sessions_for_unloaded_user(user, [activity])
            if random_state is not None:
                np.random.shuffle(sess)
            train_sessions[activity] = sess[:-test_num]
            test_sessions[activity] = sess[-test_num:]

        return train_sessions, test_sessions

    def gen_multiuser_train_test_sessions(self, users, test_num,
                                          activities=None,
                                          random_state=None):
        """
        get train_test session numbers of multiple users
        :param users: list of users
        :param test_num: numbers of test sessions
        :param activities: selected activities
        :param random_state: None (no random), or random seed
        :return: train session number list and test sessions number list
        :return: dict of user train-test sessions
        """
        result = dict()
        for user in users:
            train, test = self.gen_train_test_sessions(user, test_num,
                                                       activities, random_state)
            result[user] = {'train': train, 'test': test}
        return result

    def gen_user_set(self, num_users, exclude=None, random_state=None):
        user_pool = self.users if exclude is None else sorted(list(
            set(self.users) - set(exclude)))
        if random_state is not None:
            np.random.seed(random_state)
        return np.random.permutation(user_pool)[:num_users].tolist()

    # The below codes try to split touch sessions in different ways
    def __split_user_session(self, user_session_data, splitting_scheme):
        # splitting_scheme: (timestamp1, timestamp2, ..., timestamp)
        # all tables (dfs) insides the session are split by timestamps
        sub_data = []
        for i in range(0, len(splitting_scheme)):
            sub_dict = {}
            for key in user_session_data.keys():
                df = user_session_data[key]
                if i > 0:
                    sub_df = df[(df['time'] >= splitting_scheme[i - 1]) & (df['time'] <= splitting_scheme[i])]
                else:
                    sub_df = df[(df['time'] <= splitting_scheme[i])]
                sub_dict[key] = sub_df.copy()
            sub_data.append(sub_dict)
        return sub_data

    def __split_scheme_by_touch_swipe(self, touch_df, kfold):
        ids = touch_df['swipe_id'].unique()
        num_swipes_per_fold = math.ceil(len(ids) / kfold)
        time_splits = []
        for i in range(0, kfold):
            sid_min = math.inf
            sid_max = 0
            for sid in ids[i * num_swipes_per_fold: (i + 1) * num_swipes_per_fold]:
                sid_min = min(sid, sid_min)
                sid_max = max(sid, sid_max)
            sub_df = touch_df[(touch_df['swipe_id'] <= sid_max) & (touch_df['swipe_id'] >= sid_min)]
            # get the last timestamp of the last swipe of the ith fold
            time_splits.append(sub_df['time'].iloc[-1])
        return time_splits

    def __get_swipe_timestamps(self, touch_df):
        # get all start and end timestamps for touch_Df
        ids = touch_df['swipe_id'].unique()
        swipe_dict = {}
        for i, sid in enumerate(ids):
            sub_df = touch_df[(touch_df['swipe_id'] == sid)]
            if i > 0:
                sub_df_last = touch_df[(touch_df['swipe_id'] == ids[i - 1])]
                last_swipe_timetamp_end = sub_df_last['time'].iloc[-1]
            else:
                last_swipe_timetamp_end = sub_df['time'].iloc[1] - 1
            # get the last timestamp of the last swipe of the ith fold
            swipe_dict[sid] = (last_swipe_timetamp_end, sub_df['time'].iloc[-1])
        print(swipe_dict)
        return swipe_dict

    def __del_short_swipe(self, touch_df, order_ids, min_length, min_angle):
        """
        Delete all swipes that are less than min_length or its angle to horizontal axis less than min_angle
        """
        del_ids = []

        for sid in order_ids:
            sub_df = touch_df[(touch_df['swipe_id'] == sid)]
            x_start = sub_df['x'].iloc[1]
            y_start = sub_df['y'].iloc[1]
            x_end = sub_df['x'].iloc[-1]
            y_end = sub_df['y'].iloc[-1]
            swipe_len = math.sqrt(((x_start - x_end)**2) + ((y_start-y_end)**2))
            if swipe_len <= min_length:
                del_ids.append(sid)
                order_ids = order_ids[order_ids != sid]
            else:
                swipe_angle = 90
                if x_start != x_end:
                    swipe_angle = math.atan(abs(y_start-y_end)/abs(x_start - x_end)) * 180 / math.pi
                    if swipe_angle <= min_angle:
                        del_ids.append(sid)
                        order_ids = order_ids[order_ids != sid]

        df = touch_df.copy(deep=True)
        for del_id in del_ids:
            df = df[(df['swipe_id'] != del_id)]

        return df, order_ids

    def __shuffle_swipes(self, ids, seed):
        """
        Shuffle the swipes(ids) randomly
        """
        random.seed(seed)
        random.shuffle(ids)
        return ids

    def __gen_new_world(self, user_session_data, split_n=5, min_length=0, min_angle=0):
        """
        Return swipes data after randomly shuffled swipes
        """
        touch_df = user_session_data['touch']
        swipe_dict = self.__get_swipe_timestamps(touch_df)
        ids = touch_df['swipe_id'].unique()
        order_ids = self.__shuffle_swipes(ids, 7)
        del_touch_df, order_ids = self.__del_short_swipe(touch_df, order_ids, min_length, min_angle)
        partitions = [swipe_dict[i] for i in order_ids]

        num_swipes_per_fold = math.ceil(len(order_ids) / split_n)
        sub_data = []
        for i in range(0, split_n):
            time_min = math.inf
            local_min = math.inf
            split_ranges = partitions[i * num_swipes_per_fold: (i + 1) * num_swipes_per_fold]
            sub_dict = {}
            for time_range in split_ranges:
                time_min = min(time_min, time_range[0])
                local_min = time_min

            warnings.filterwarnings("ignore")
            for key in user_session_data.keys():
                sub_df = None
                time_min = local_min
                for time_range in split_ranges:
                    if key == 'touch':
                        df = del_touch_df
                    else:
                        df = user_session_data[key]
                    if sub_df is None:
                        sub_df = df[(df['time'] > time_range[0]) & (df['time'] <= time_range[1])]
                        sub_df['time'] = time_min - time_range[0] + sub_df['time']
                    else:
                        concate_df = df[(df['time'] > time_range[0]) & (df['time'] <= time_range[1])]
                        concate_df['time'] = time_min - time_range[0] + concate_df['time']
                        sub_df = pd.concat([sub_df, concate_df])
                    time_min = time_min + time_range[1] - time_range[0]
                sub_dict[key] = sub_df.copy()
            sub_data.append(sub_dict)
        return sub_data

    def split_data_by(self, session, scheme_name, sel_users=None, min_length=0, min_angle=0):
        """
        Split swipes depends on scheme_name
        """
        new_data = {}
        sub_data = []
        for user in sel_users:
            session_data = self.data[user][session]
            if scheme_name == "split_by_touch_swipe":
                scheme = self.__split_scheme_by_touch_swipe(session_data['touch'], 5)
                sub_data = self.__split_user_session(session_data, scheme)
            elif scheme_name == "shuffle_split":
                sub_data = self.__gen_new_world(session_data, split_n=5, min_length=min_length, min_angle=min_angle)

            new_session_naming_list = []
            for i in range(0, len(sub_data)):
                new_session_naming_list.append(100 + i)

            new_data[user] = {
                session: sub_data[i]
                for i, session in enumerate(new_session_naming_list)
            }

        return new_data

    def __init__(self, data_file_path, phone_device, sel_users=None):
        self.path = os.path.join(data_file_path, "BBMAS_(" + phone_device + ")")
        self.users = self.__load_all_users()
        # self.data = self.load_dataset(sel_users)
        self.data = dict()
        self.device = phone_device


# Code for testing locally
# if __name__ == "__main__":
#     # test code
#     bp = BBMASProvider("C:/UWaterloo/4A/URA - 2/small_bbmas/new_dataset", "Samsung_S6")
#     users = bp.get_all_users()
#     print(users)
#     bp.load([users[0]])  # load("1")
#     print(bp.data.keys())
#     print(bp.data['1'].keys())
#     print(bp.data['1'][1].keys())
#     print(bp.data['1'][1]['touch'])
#     # print(bp.data['1'][3]['acc'])
#     # print(bp.data['1'][2]['touch'])
#     print(bp.get_user_sessions_with_touch_activity('1', bp.ACTIVITY_TYPING))
#     # print("test split data by")
#     # new_data = bp.split_data_by(1, "shuffle_split", ['1'], min_length=5, min_angle=30)
#     print("================================")
#     print(bp.get_sessions_for_unloaded_user("1",
#                                          [bp.ACTIVITY_TYPING]))
#
#     # bp = BBMASProvider("C:/UWaterloo/4A/URA - 2/small_bbmas/dataset", "C:/UWaterloo/4A/URA - 2/small_bbmas/new_dataset",
#     #                    "HTC_One")
#     # users = bp.get_all_users()
#     # bp.load(users)  # load("1")
# #     print(bp.get_users_by_device("Samsu
