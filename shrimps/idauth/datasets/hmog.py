import os
import pandas as pd
import numpy as np


class HMOGProvider():

    ACTIVITY_READING_SITTING = 0
    ACTIVITY_READING_WALKING = 1
    ACTIVITY_WRITING_SITTING = 2
    ACTIVITY_WRITING_WALKING = 3
    ACTIVITY_MAP_SITTING =4
    ACTIVITY_MAP_WALKING = 5

    SUPPORTED_ACTIVITIES = [
        ACTIVITY_READING_SITTING,
        ACTIVITY_READING_WALKING,
        ACTIVITY_WRITING_SITTING,
        ACTIVITY_WRITING_WALKING
    ]

    SUPPORTED_SENSORS = [
        'acc', 'gyro', 'touch'
    ]

    def __get_user_session_dir(self, user, session):
        return os.path.join(self.path, str(user),
                            str(user) + "_session_" + str(session))

    def get_task_desc(self, task_id):
        if task_id in [1, 7, 13, 19]:
            return self.ACTIVITY_READING_SITTING
        elif task_id in [2, 8, 14, 20]:
            return self.ACTIVITY_READING_WALKING
        elif task_id in [3, 9, 15, 21]:
            return self.ACTIVITY_WRITING_SITTING
        elif task_id in [4, 10, 16, 22]:
            return self.ACTIVITY_WRITING_WALKING
        elif task_id in [5, 11, 17, 23]:
            return self.ACTIVITY_MAP_SITTING
        elif task_id in [6, 12, 18, 24]:
            return self.ACTIVITY_MAP_WALKING
        else:
            raise ValueError("Invalid Activity ID received: " + str(task_id))

    def get_activityID_to_task_desc(self, user, session,
                                    filename="Activity.csv"):
        """Maps activityID to activity description"""
        touch_data_path = os.path.join(
            self.__get_user_session_dir(user, session),
            filename)
        activity = dict()
        with open(touch_data_path) as f:
            for l in f:
                tokens = [int(x) for x in l.split(',')]
                activity[tokens[0]] = self.get_task_desc(tokens[-2])
                activity[tokens[0] + 100000] = self.get_task_desc(tokens[-2])
        return activity

    def get_user_sessions_with_activity(self, user, activity):
        return [i for i, session in self.data[user].items()
                if session['acc']['activity'].iloc[0] == activity]

    def get_sessions_by_activities(self, user, activities):
        return [i for i, session in self.data[user].items()
                if session['acc']['activity'].iloc[0] in activities]

    def get_sessions_for_unloaded_user(self, user, activities):
        sessions = self.__load_all_sessions_of_user(user, self.path)
        result = []
        for session in sessions:
            task_desc = self.get_activityID_to_task_desc(user, session)
            file_complete_path = os.path.join(
                self.__get_user_session_dir(user, session), "Accelerometer.csv")
            with open(file_complete_path, 'r') as fp:
                line = fp.readline().split(',')
                if len(line) < 3:
                    print(user, session, "lacks acc data.")
                    continue
                # print(line)
                if task_desc[int(line[2])] in activities:
                    result.append(session)
        return result

    def __load_all_users(self, path=None):
        """
        Obtain a list of all users under the directory
        """
        to_open = path if path is not None else self.path
        return [i for i in os.listdir(to_open)
                if os.path.isdir(os.path.join(to_open, i))]

    def __load_all_sessions_of_user(self, user, path=None):
        to_open = path if path is not None else self.path
        result = [int(i.split("_")[-1]) for i in
                  os.listdir(os.path.join(to_open, user))
                  if os.path.isdir(os.path.join(to_open, user, i))]
        return sorted(result)

    def get_all_users(self):
        return self.users

    def load_touch_data(self, user, session, task_desc, 
                        filename="ScrollEvent.csv"):
        """load touch data for a single user-session"""
        file_complete_path = os.path.join(
            self.__get_user_session_dir(user, session),
            filename)

        # task_desc = self.get_activityID_to_task_desc(user, session)
        with open(file_complete_path, 'r') as fp:
            result = []
            for line in fp:
                l = [float(x) for x in line.split(',')]
                item = {
                    'swipe_id': int(l[4]),
                    'time': l[0],
                    'x': l[11],
                    'y': l[12],
                    'pressure': l[13],
                    'area': l[14],
                    'orientation': l[-1],
                    'activity': task_desc[int(l[3])],
                }
                result.append(item)
            return pd.DataFrame(result)

    def load_motion_data(self, user, session, task_desc, filename):
        """load acc/gyro/magn for a single user-session"""
        file_complete_path = os.path.join(
            self.__get_user_session_dir(user, session),
            filename)
        # task_desc = self.get_activityID_to_task_desc(user, session)
        with open(file_complete_path, 'r') as fp:
            result = []
            for line in fp:
                l = [float(x) for x in line.split(',')]
                item = {
                    'time': l[0],
                    'x': l[3],
                    'y': l[4],
                    'z': l[5],
                    # 'orientation': l[6],
                    'activity': task_desc[int(l[2])]
                }
                result.append(item)
            return pd.DataFrame(result)

    def load_keystroke_data(self, user, session, task_desc, 
                            filename="KeyPressEvent.csv"):
        file_complete_path = os.path.join(
            self.__get_user_session_dir(user, session),
            filename)
        # task_desc = self.get_activityID_to_task_desc(user, session)
        with open(file_complete_path, 'r') as fp:
            result = []
            for line in fp:
                l = [int(x) for x in line.split(',')]
                item = {
                    'time': l[0],
                    'moment': l[1],
                    'type': l[3],
                    'key': l[4],
                    'activity': task_desc[int(l[2])]
                }
                result.append(item)
            return pd.DataFrame(result)

    def load_single_user(self, user):
        assert user in self.users
        if user in self.data:
            return

        # obtain all sessions of a user
        sessions = self.__load_all_sessions_of_user(user, self.path)
       
        result = dict()
        for session in sessions:
            task_desc = self.get_activityID_to_task_desc(user, session)
            item = {
                'touch': self.load_touch_data(user, session,
                                              task_desc),
                'acc': self.load_motion_data(user, session,
                                             task_desc,
                                             "Accelerometer.csv"),
                'gyro': self.load_motion_data(user, session,
                                              task_desc,
                                              "Gyroscope.csv"),
                'keystroke': self.load_keystroke_data(user, session,
                                                      task_desc),
                # 'magn': self.load_motion_data(user, session,
                #                              "Magnetometer.csv")
            }

            result[session] = item
        self.data[user] = result

    def load(self, sel_users=None):
        user_pool = self.users if sel_users is None else sel_users
        for user in user_pool:
            self.load_single_user(user)


    """ useful functions """

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
    
    def gen_negative_trainer_set(self, num_testers, random_state=None):
        if random_state is not None:
            np.random.seed(random_state)
        return np.random.permutation(self.users)[:num_testers].tolist()
    
    def gen_user_set(self, num_users, exclude=None, random_state=None):
        user_pool = self.users if exclude is None else sorted(list(
            set(self.users) - set(exclude)))
        if random_state is not None:
            np.random.seed(random_state)

        return np.random.permutation(user_pool)[:num_users].tolist()
            
    def __init__(self, data_file_path, sel_users=None):
        self.path = data_file_path
        self.users = self.__load_all_users()
        # self.data = self.load_dataset(sel_users)
        self.data = dict()
