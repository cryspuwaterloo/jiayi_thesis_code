"""
BBMAS Parser

BB-MAS dataset link: https://ieee-dataport.org/open-access/su-ais-bb-mas-syracuse-university-and-assured-information-security-behavioral-biometrics

BB-MAS dataset contains user behavioral data in one session.

BBMAS structure

data[user][session] -> user session dict

user session dict:
    - device
    - sensor_records

Example:

    # bbmas = BBMASProvider()
    user_session = bbmas.data['alice'][0]
    # user_session['available_sensors'] --> 'acc', 'gyro'
    user_session['device'] --> 'Google Pixel'
    user_session[

Note BBMAS is different from other database -- each user contributes to
one session but two devices. We put each device's data into a separate
session.
"""

import os
import pandas as pd
import numpy as np
import datetime as dt


class BBMASProvider:

    def get_all_users(self):
        return [i for i in os.listdir(self.data_dir)
                if os.path.isdir(os.path.join(self.data_dir, i)) and
                i.isdigit()]

    def get_device(self, uid, type):
        device_name = "unknown_" + type
        user_path = os.path.join(self.data_dir, str(uid))
        files = os.listdir(user_path)
        # get phone model
        keyword = "_HandPhone_Accelerometer_" if type == "phone"\
            else "_HandTablet_Accelerometer_"

        for file in files:
            if file.startswith(str(uid) + keyword):
                device_name = file.split("(")[1].split(")")[0]
                # print(phone_name)
                break

        return device_name

    def get_users_by_device(self, device):
        return [user for user in self.users
                if self.get_device(user, 'phone') == device
                or self.get_device(user, 'tablet') == device]


    def parse_files(self, uid):
        user_path = os.path.join(self.data_dir, str(uid))
        files = os.listdir(user_path)
        # get phone model
        phone_name = self.get_device(uid, "phone")

        # tablet_name = "unknown_tablet"
        # for file in files:
        #     if file.startswith(str(uid) + "_HandTablet_Accelerometer_"):
        #         tablet_name = file.split("(")[1].split(")")[0]
        #         print(tablet_name)
        #         break

        result_phone = dict()
        result_phone['device'] = phone_name
        result_phone['uid'] = uid
        result_phone['touch'] = BBMASProvider.read_touch_file(
            os.path.join(user_path, str(uid) + "_HandPhone_TouchEvent_("
                         + phone_name + ").csv"))
        result_phone['acc'] = BBMASProvider.read_motion_file(
            os.path.join(user_path, str(uid) + "_HandPhone_Accelerometer_("
                         + phone_name + ").csv"))
        result_phone['gyro'] = BBMASProvider.read_motion_file(
            os.path.join(user_path, str(uid) + "_HandPhone_Gyroscope_("
                         + phone_name + ").csv"))

        return result_phone

    @staticmethod
    def read_motion_file(filename):
        data = pd.read_csv(filename, index_col="EID")
        data.rename(columns={'Xvalue': 'x', 'Yvalue': 'y', 'Zvalue': 'z'},
                   inplace=True)
        data['time'] = (pd.to_datetime(data['time'])
                        - dt.datetime(1970, 1, 1)).dt.total_seconds() * 1000
        data['time'] = data['time'].astype("int64")
        data = data[['time', 'x', 'y', 'z']]
        return data


    @staticmethod
    def read_touch_file(filename):
        swipe_id = 0
        with open(filename, 'r') as fp:
            line = fp.readline()

            output = []
            line = fp.readline()
            prev_time = -1
            prev_action = -1
            while (line):
                # process line
                content = line.strip().split(",")
                x = float(content[1])
                y = float(content[2])
                pressure = float(content[3])
                t_major = float(content[4])
                t_minor = float(content[5])
                # note that width is specific to different devices, here just keep it
                width = np.average([t_major, t_minor])
                pt_id = int(content[6])
                orientation = float(content[7])
                action = prev_action if len(content[8]) == 0 else int(
                    float(content[8]))

                if action == 1:
                    swipe_id += 1

                time_raw = str(content[9])
                if time_raw == "null":
                    time = prev_time
                else:
                    time_coms = time_raw.split(':')
                    new_time_raw = ':'.join(time_coms[:-1]) + "." + time_coms[
                        -1]
                    time = (pd.to_datetime(new_time_raw) - dt.datetime(1970, 1,
                                                                       1)).total_seconds() * 1000
                prev_time = time
                prev_action = action
                # print(x, y, t_major, t_minor, width, time_raw, time)

                if action == 2:
                    output.append({
                        'swipe_id': swipe_id,
                        'sys_time': time,
                        'x': x,
                        'y': y,
                        'pressure': pressure,
                        'width': width,
                        'orientation': orientation,
                        # 'action': action,
                        'activity': "None" #TODO: EXTRACT ACTIVITY
                    })




                line = fp.readline()
            return pd.DataFrame(output)


    def load_one_user_session(self, user, session=None):
        """read one user-session (actually two given different devices"""
        if user not in self.cached_data:
            self.cached_data[user] = self.parse_files(user)


    def load(self, users, sel_sessions=None):
        for user in users:
            self.load_one_user_session(user)


    def __init__(self, path):
        self.data_dir = path
        self.users = self.get_all_users()
        self.cached_data = dict()



if __name__ == "__main__":
    # test code
    bp = BBMASProvider("H:/external_dataset/BB-MAS_Dataset")
    bp.load_one_user_session("2")
    print(bp.get_users_by_device("Samsung_S6"))

