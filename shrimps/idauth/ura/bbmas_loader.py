import os
import warnings

import pandas as pd
import numpy as np
from datetime import datetime
from dateutil import parser


def rreplace(s, old, new, count):
    return (s[::-1].replace(old[::-1], new[::-1], count))[::-1]


class BBMASLoader:
    def __time_conversion(self, utc_time, type):
        """
        Convert utc_time string to timestamp
        """
        if utc_time != "null\n" and utc_time != "":
            utc_time = utc_time[:-1] if type != "t" else rreplace(utc_time[:-1], ":", ".", 1)
            datetime_obj = datetime.strptime(utc_time, '%Y-%m-%d %H:%M:%S.%f')
            unix_timestmp = datetime_obj.timestamp()
            return unix_timestmp
        return 0

    def __get_user_session_dir(self, user, event):
        result = [i for i in
                  os.listdir(os.path.join(self.path, user))
                  if (os.path.isdir(os.path.join(self.path, user)) and event in i)]
        if result:
            return os.path.join(self.path, user, result[0])
        else:
            return None

    def __get_check_points_dir(self, user, event):
        result = [i for i in
                  os.listdir(os.path.join(self.path, user))
                  if (os.path.isdir(os.path.join(self.path, user)) and event in i)]
        return os.path.join(self.path, user, result[0])

    def __load_all_users(self, path=None):
        """
        Obtain a list of all users under the directory
        """
        to_open = path if path is not None else self.path
        return [i for i in os.listdir(to_open)
                if os.path.isdir(os.path.join(to_open, i))]  # C:/UWaterloo/4A/URA - 2/small_bbmas/dataset\#1

    def __load_all_sessions_of_user(self, user, path=None):
        """
        Obtain a list of all sessions for user
        """
        return [str(i) for i in range(1, 7)]

    def __check_time_list(self, check_path):
        with open(check_path, 'r') as fp:
            check_time_list = []
            count = 0
            in_time = 0
            out_time = 0
            for i, line in enumerate(fp):
                if count == 0:
                    count += 1
                    continue
                l = [x for x in line.split(',')]
                if l[1] in ["DoorEntry"]:
                    out_time = self.__time_conversion(l[-1], "c")
                    check_time_list.append((in_time, out_time, count))
                    count += 1
                elif l[1] in ["DoorExit", "StairEntry"]:
                    in_time = self.__time_conversion(l[-1], "c")

        print(check_time_list)
        return check_time_list

    def __check_act(self, check_list, time):
        """
        Return the corresponding activity
        """
        activity = ""
        for i, check_tuple in enumerate(check_list):
            if time <= check_tuple[0]:
                activity = str(check_tuple[1])
                break
            if i == len(check_list) - 1:
                activity = str(chr(ord(check_tuple[1]) + 1))
        return activity

    def get_all_users(self):
        """
        Return a list of all users
        """
        return self.users

    def load_touch_data(self, user, check_list, event, filename="TouchEvent.csv"):
        """
        Load touch data for a single user-session
        """
        file_complete_path = self.__get_user_session_dir(user, event)
        time = 0
        sid = 0

        if not file_complete_path:
            return None
        with open(file_complete_path, 'r') as fp:
            result = []
            count = 0
            flag = 0
            for line in fp:
                if count == 0:
                    count += 1
                    continue
                l = [x for x in line.split(',')]
                if (l[-1] != 'null\n'):
                    time = l[-1]
                if flag == 1:
                    sid += 1
                    flag = 0
                if l[8] == '1.0':
                    flag = 1
                item = {
                    'time': self.__time_conversion(time, "t"),
                    'x': float(l[1]),
                    'y': float(l[2]),
                    'pressure': float(l[3]),
                    'touchMajor': float(l[4]),
                    'touchMinor': float(l[5]),
                    'orientation': float(l[7]),
                    'swipe_id': sid,
                    'activity': 1
                }
                result.append(item)
        df = pd.DataFrame(result)
        warnings.filterwarnings("ignore")
        for i, checks in enumerate(check_list):
            df1 = df[(checks[0] < df['time']) & (df['time'] < checks[1])]
            df1.loc[df1['activity'] == 1, 'activity'] = checks[2]
            filedir = user + "_session_" + str(checks[2])
            write_path = os.path.join(self.new_path, "BBMAS_("+self.device+")", user, filedir, filename)
            if not os.path.exists(os.path.dirname(write_path)):
                os.makedirs(os.path.dirname(write_path))
            df1.to_csv(write_path)
        # return result

    def load_motion_data(self, user, check_list, event, filename="Accelerometer.csv"):
        """
        Load acc/gyro for a single user-session
        """
        file_complete_path = self.__get_user_session_dir(user, event)

        if not file_complete_path:
            return None

        with open(file_complete_path, 'r') as fp:
            result = []
            count = 0
            for line in fp:
                if count == 0:
                    count += 1
                    continue
                l = [x for x in line.split(',')]
                item = {
                    'time': self.__time_conversion(l[-1], "a"),
                    'x': float(l[1]),
                    'y': float(l[2]),
                    'z': float(l[3]),
                    'activity': 1
                }
                result.append(item)
        df = pd.DataFrame(result)
        warnings.filterwarnings("ignore")
        for i, checks in enumerate(check_list):
            df1 = df[(checks[0] < df['time']) & (df['time'] < checks[1])]
            df1.loc[df1['activity'] == 1, 'activity'] = checks[2]
            filedir = user + "_session_" + str(checks[2])
            write_path = os.path.join(self.new_path, "BBMAS_("+self.device+")", user, filedir, filename)
            if not os.path.exists(os.path.dirname(write_path)):
                os.makedirs(os.path.dirname(write_path))
            df1.to_csv(write_path)

    def load_keystroke_data(self, user, check_list, event, filename="Keyboard.csv"):
        """
        Load keystroke for a single user-session
        """
        file_complete_path = self.__get_user_session_dir(user, event)

        if not file_complete_path:
            return None

        with open(file_complete_path, 'r') as fp:
            result = []
            count = 0
            last_line = []
            for line in fp:
                key_ascii = 0
                if count == 0:
                    count += 1
                    continue
                if line[-2] == '"':
                    last_line = line
                    continue
                elif line[0] == '"':
                    line = last_line + line
                    key_ascii = 10
                l = [x for x in line.split(',')]
                if l[1] == "SPACE":
                    key_ascii = 32
                elif l[1] == "BACKSPACE":
                    key_ascii = 8
                elif len(l[1]) == 1:
                    key_ascii = ord(l[1])
                item = {
                    'time': self.__time_conversion(l[-1], "a"),
                    'key': key_ascii,
                    'activity': 1
                }
                result.append(item)
        df = pd.DataFrame(result)
        warnings.filterwarnings("ignore")
        for i, checks in enumerate(check_list):
            df1 = df[(checks[0] < df['time']) & (df['time'] < checks[1])]
            df1.loc[df1['activity'] == 1, 'activity'] = checks[2]
            filedir = user + "_session_" + str(checks[2])
            write_path = os.path.join(self.new_path, "BBMAS_("+self.device+")", user, filedir, filename)
            if not os.path.exists(os.path.dirname(write_path)):
                os.makedirs(os.path.dirname(write_path))
            df1.to_csv(write_path)

    def load_single_user(self, user):
        assert user in self.users
        if user in self.data:
            return
        result = dict()

        act = {'session': [1, 2, 3, 4, 5, 6],
               'activity': ['typing', 'corridor_walking', 'stair_up',
                            'corridor_walking', 'stair_down', 'corridor_walking']}

        df_act = pd.DataFrame(act, columns=['session', 'activity'])

        write_path = os.path.join(self.new_path, "BBMAS_("+self.device+")", "Activity.csv")
        if not os.path.exists(write_path):
            os.makedirs(os.path.dirname(write_path))
        df_act.to_csv(write_path)

        sessions = self.__load_all_sessions_of_user(user, self.path)
        print(sessions)

        file_check_path = self.__get_check_points_dir(user, "_HandPhone_Checkpoints_")
        check_list = self.__check_time_list(file_check_path)

        result['touch'] = self.load_touch_data(user, check_list, "_HandPhone_TouchEvent_(" + self.device + ").csv")
        result['acc'] = self.load_motion_data(user, check_list, "_HandPhone_Accelerometer_(" + self.device + ").csv", "Accelerometer.csv")
        result['gyro'] = self.load_motion_data(user, check_list, "_HandPhone_Gyroscope_(" + self.device + ").csv", "Gyroscope.csv")
        result['keystroke'] = self.load_keystroke_data(user, check_list, "_HandPhone_Keyboard_(" + self.device + ").csv")
        self.data[user] = result

    def load(self, sel_users=None):
        """
        load the data field of bbmas, sel_users is a list of users wish to load
        """
        user_pool = self.users if sel_users is None else sel_users
        for user in user_pool:
            self.load_single_user(user)

    def __init__(self, data_file_path, data_new_path, phone_device, sel_users=None):
        self.path = data_file_path
        self.new_path = data_new_path
        self.users = self.__load_all_users()
        # self.data = self.load_dataset(sel_users)
        self.data = dict()
        self.device = phone_device



# For testing locally
# if __name__ == "__main__":
#     # test code
#     bp = BBMASLoader("C:/UWaterloo/4A/URA - 2/small_bbmas/dataset", "C:/UWaterloo/4A/URA - 2/small_bbmas/new_dataset",
#                        "Samsung_S6")
#     users = bp.get_all_users()
#     bp.load(users[0])  # load("1")
#
#     # bp = BBMASLoader("C:/UWaterloo/4A/URA - 2/small_bbmas/dataset", "C:/UWaterloo/4A/URA - 2/small_bbmas/new_dataset",
#     #                    "HTC_One")
#     # users = bp.get_all_users()
#     # bp.load(users)  # load("1")
# #     print(bp.get_users_by_device("Samsung_S6"))
