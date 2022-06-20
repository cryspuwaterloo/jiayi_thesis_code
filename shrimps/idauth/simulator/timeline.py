import os


class Timeline:
    END_TIME_OFFSET = 20 # next session starts 20 seconds after the current one

    def sort_events(self):
        self.events = sorted(self.events)
        # each time events are sorted, the pointer will be reset
        self.reset()


    def add_user_session(self, user, activity, events):
        self.events += events
        new_end_time = events[-1][0] + self.END_TIME_OFFSET
        self.user_groundtruth.append((self.end_time,
                                      new_end_time,
                                      user,
                                      activity))
        self.end_time = new_end_time

    def add_special_event(self, event_type, event_data=None):
        self.events.append((self.end_time, event_type, event_data))

    def compare_groundtruth(self, time, pred):
        for st, ed, user, activity in self.user_groundtruth:
            if time >= st and time < ed:
                return pred == user
        print("Not found")
        return 0

    def get_groundtruth_result(self, time):
        """
        get the groundtruth user with a given timestamp
        :param time:
        :return:
        """
        for st, ed, user, activity in self.user_groundtruth:
            if time >= st and time < ed:
                return user
        print("Not found")
        return 0


    def next(self):
        """
        Obtain the next event
        :return: event (time, type, values)
        """
        # obtain next event
        if self.current_event >= len(self.events):
            return

        event = self.events[self.current_event]
        self.current_event += 1
        return event

    def reset(self):
        """
        reset the pointer to the first event
        :return:
        """
        self.current_event = 0

    def __iter__(self):
        for item in self.events:
            yield item


    def save(self, name="default_timeline", out_path="."):
        # save the current timeline
        meta_file = os.path.join(out_path, name + ".meta")
        timeline_file = os.path.join(out_path, name + ".tml")
        with open(meta_file, 'w') as fp:
            fp.write(str(self.base_time) + "," + str(self.end_time) + "\n")
            # keep ground truth
            for st, ed, user, activity in self.user_groundtruth:
                fp.write(str(st) + "," + str(ed) + ","
                         + str(user) + "," + str(activity) + "\n")
        with open(timeline_file, 'w') as fp:
            for event in self.events:
                fp.write(str(event[0]) + "," + str(event[1]) + ","
                         + ",".join([str(item) for item in event[2]]) + "\n")

    @staticmethod
    def load(name, in_path="."):
        meta_file = os.path.join(in_path, name + ".meta")
        timeline_file = os.path.join(in_path, name + ".tml")
        tml = Timeline()
        with open(meta_file, 'r') as fp:
            lines = fp.readlines()
            elems = lines[0].strip().split(",")
            tml.base_time = int(elems[0])
            tml.end_time = int(elems[1])
            for line in lines[1:]:
                gt_items = line.strip().split(",")
                tml.user_groundtruth.append((
                    int(gt_items[0]),
                    int(gt_items[1]),
                    gt_items[2],
                    int(gt_items[3])
                ))

        with open(timeline_file, 'r') as fp:
            for line in fp:
                elems = line.strip().split(",")
                content = []
                if elems[1] in ['acc', 'gyro']:
                    content = [float(item) for item in elems[2:]]
                elif elems[1] == "touch":
                    content = [float(i)
                               for i in elems[2:10]] + [int(i)
                                                        for i in elems[10:]]
                tml.events.append([
                    int(elems[0]),
                    str(elems[1]),
                    content
                ])
        return tml

    def __init__(self, base_time=None):
        self.base_time = base_time if base_time is not None else 0
        self.end_time = self.base_time
        self.events = []
        self.user_groundtruth = []
        self.current_event = 0
        # user groundtruth = [(start_time, end_time, user, activity)]
