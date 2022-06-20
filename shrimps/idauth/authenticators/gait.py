"""
Deep learning gait authenticator is base on
https://github.com/qinnzou/Gait-Recognition-Using-Smartphones

We modify the original code to make it run in TF2.x
"""

from idauth.authenticators.base import Authenticator


import os
import numpy as np
from scipy.signal import find_peaks
from scipy.interpolate import interp1d
import tensorflow as tf
from sklearn.metrics import classification_report, precision_score,\
    recall_score, f1_score, accuracy_score, roc_auc_score
from idauth.utils.metrics import multiclass_auc, multiclass_eer
from idauth.utils.helpers import sparse_label_converter

EXAMPLE_DEEP_GAIT_CONFIG = {
    'n_hidden': 64,
    'n_classes': 5,
}


class DeepGaitModel(tf.keras.Model):
    """
    Deep learning model
    input size: [batch_size,
    """
    def __init__(self, config):
        super(DeepGaitModel, self).__init__()
        self.config = config

        self.lstm1 = tf.keras.layers.LSTM(config['n_hidden'],
                                          return_sequences=True)
        self.lstm2 = tf.keras.layers.LSTM(config['n_hidden'],
                                          return_sequences=False,
                                          return_state=False)
        # self.dense_lstm = tf.keras.layers.Dense(512, activation='relu')

        self.conv1 = tf.keras.layers.Conv2D(32, 3, padding='same',
                                            activation='elu')
        self.pool1 = tf.keras.layers.MaxPool2D((2, 2), strides=(2, 2),
                                               padding='same')
        self.conv2 = tf.keras.layers.Conv2D(64, 3, padding='same',
                                            activation='elu')
        self.pool2 = tf.keras.layers.MaxPool2D((2, 2), strides=(2, 1),
                                               padding='same')
        self.dense1 = tf.keras.layers.Dense(64, activation='elu')
        self.dense2 = tf.keras.layers.Dense(config['n_classes'],
                                            activation='softmax')

    def call(self, x, training=None, mask=None):
        # lstm
        lstm_output0 = self.lstm1(x)
        lstm_output = self.lstm2(lstm_output0)
        # lstm_output = self.dense_lstm(lstm_output1)

        cnn_input = tf.reshape(x, [-1, x.shape[1], x.shape[2], 1])
        # cnn net
        cnn_output0 = self.conv1(cnn_input)
        cnn_output1 = self.pool1(cnn_output0)
        cnn_output2 = self.conv2(cnn_output1)
        cnn_output3 = self.pool2(cnn_output2)
        cnn_output4 = tf.reshape(cnn_output3, [-1, 32 * 3 * 64])
        cnn_output = self.dense1(cnn_output4)
        # final classification
        final_input = tf.concat([lstm_output, cnn_output], axis=1)
        output = self.dense2(final_input)
        return output

    def get_final_layer(self, x):
        # lstm
        lstm_output0 = self.lstm1(x)
        lstm_output = self.lstm2(lstm_output0)
        # lstm_output = self.dense_lstm(lstm_output1)

        cnn_input = tf.reshape(x, [-1, x.shape[1], x.shape[2], 1])
        # cnn net
        cnn_output0 = self.conv1(cnn_input)
        cnn_output1 = self.pool1(cnn_output0)
        cnn_output2 = self.conv2(cnn_output1)
        cnn_output3 = self.pool2(cnn_output2)
        cnn_output4 = tf.reshape(cnn_output3, [-1, 32 * 3 * 64])
        cnn_output = self.dense1(cnn_output4)
        # final classification
        final_input = tf.concat([lstm_output, cnn_output], axis=1)
        return final_input


class DeepGaitExtractionModel(tf.keras.Model):
    """
    Deep Learning model to extract gait data clips from data
    input size is [batch_size, input_size 6, sample_size 1024, 1]
    output: [batch_size, sample_size] indicating if each sample
    belongs to a gait
    """
    def _fixed_pad(self, x):
        return tf.pad(tensor=x, paddings=[[0, 0], [0, 0], [7, 8], [0, 0]],
                      mode="REFLECT")

    def __init__(self):
        super(DeepGaitExtractionModel, self).__init__()

        self.conv1_1 = tf.keras.layers.Conv2D(64, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')
        self.conv1_2 = tf.keras.layers.Conv2D(64, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')
        self.conv2_1 = tf.keras.layers.MaxPooling2D(pool_size=[1, 2],
                                                    strides=[1, 2],
                                                    padding='valid')
        self.conv2_2 = tf.keras.layers.Conv2D(128, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')
        self.conv2_3 = tf.keras.layers.Conv2D(128, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')
        self.conv3_1 = tf.keras.layers.MaxPooling2D(pool_size=[1, 2],
                                                    strides=[1, 2],
                                                    padding='valid')
        self.conv3_2 = tf.keras.layers.Conv2D(256, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')
        self.conv3_3 = tf.keras.layers.Conv2D(256, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')
        self.conv3_4 = tf.keras.layers.Conv2D(256, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')
        self.conv2_4_1 = tf.keras.layers.Conv2DTranspose(128,
                                                         kernel_size=[1, 2],
                                                         strides=[1, 2],
                                                         padding="VALID")
        self.conv2_5 = tf.keras.layers.Conv2D(128, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')
        self.conv2_6 = tf.keras.layers.Conv2D(128, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')

        self.conv1_3_1 = tf.keras.layers.Conv2DTranspose(128,
                                                         kernel_size=[1, 2],
                                                         strides=[1, 2],
                                                         padding="VALID")
        self.conv1_4 = tf.keras.layers.Conv2D(64, kernel_size=[1, 16],
                                              padding='valid',
                                              activation='relu')
        self.conv1_4_2 = tf.keras.layers.Conv2D(64, kernel_size=[1, 16],
                                                padding='valid',
                                                activation='relu')
        self.conv1_5 = tf.keras.layers.Conv2D(256, kernel_size=[6, 1],
                                              padding='valid',
                                              activation='relu')
        self.conv1_6 = tf.keras.layers.Conv2D(1, kernel_size=[1, 1],
                                              padding='valid',
                                              activation='sigmoid')


    def call(self, x, training=None, mask=None):
        x = self._fixed_pad(x)
        conv1_1 = self.conv1_1(x)
        conv1_1 = self._fixed_pad(conv1_1)
        conv1_2 = self.conv1_2(conv1_1)

        conv2_1 = self.conv2_1(conv1_2)
        conv2_1 = self._fixed_pad(conv2_1)
        conv2_2 = self.conv2_2(conv2_1)
        conv2_2 = self._fixed_pad(conv2_2)
        conv2_3 = self.conv2_3(conv2_2)

        conv3_1 = self.conv3_1(conv2_3)
        conv3_1 = self._fixed_pad(conv3_1)
        conv3_2 = self.conv3_2(conv3_1)
        conv3_2 = self._fixed_pad(conv3_2)
        conv3_3 = self.conv3_3(conv3_2)
        conv3_3 = self._fixed_pad(conv3_3)
        conv3_4 = self.conv3_4(conv3_3)

        conv2_4_1 = self.conv2_4_1(conv3_4)
        conv2_4 = tf.concat([conv2_4_1, conv2_3], axis=3)
        conv2_4 = self._fixed_pad(conv2_4)
        conv2_5 = self.conv2_5(conv2_4)
        conv2_5 = self._fixed_pad(conv2_5, )
        conv2_6 = self.conv2_6(conv2_5)

        conv1_3_1 = self.conv1_3_1(conv2_6)
        conv1_3 = tf.concat([conv1_2, conv1_3_1], axis=3)
        conv1_3 = self._fixed_pad(conv1_3)
        conv1_4 = self.conv1_4(conv1_3)
        conv1_4 = self._fixed_pad(conv1_4)
        conv1_4 = self.conv1_4_2(conv1_4)
        conv1_5 = self.conv1_5(conv1_4)
        conv1_6 = self.conv1_6(conv1_5)

        out = tf.reshape(conv1_6, [-1, 1024])
        return out


class DeepGaitAuthenticator(Authenticator):
    def extract_features(self, raw_data):
        """
        This function is designed to generate input vectors for Deep Gait
        Model. It translate sample_num * channel_num matrix into batches
        :param raw_data: six channel motion data matrix
        :return: [None, 128, 6]
        """
        # TODO: Embed Gait Extraction

        # WARNING: Assume input raw_data contain gaits
        batches = fixed_gait_segmentation_128(raw_data)
        return batches

    def train_model(self, train_data, test_data=None):
        y_train, X_train = train_data
        print(y_train.shape)
        if self.model is None:
            self.model = DeepGaitModel(
                {'n_classes': y_train.shape[1],
                 'n_hidden': 64}
            )
        self.model.compile(
            optimizer=tf.keras.optimizers.Adam(),  # Optimizer
            # Loss function to minimize
            loss=tf.losses.CategoricalCrossentropy(),
            # List of metrics to monitor
            metrics=[tf.keras.metrics.CategoricalAccuracy()],
        )
        v_data = None if test_data is None else (test_data[1], test_data[0])
        self.model.fit(
            X_train,
            y_train,
            batch_size=self.profile['batch_size'],
            epochs=self.profile['epochs'],
            validation_split=0.2,
        )
        result = self.model.predict(v_data[0])
        converted_y_pred = np.argmax(result, axis=1)
        converted_y_true = np.matmul(v_data[1],
                                     np.array(range(v_data[1].shape[1])))
        self.trained = True
        return classification_report(converted_y_true, converted_y_pred)


    @staticmethod
    def write_offline_training_data(train_set, test_set, todir):
        y_train, X_train = train_set
        y_test, X_test = test_set

        np.save(os.path.join(todir, "y_train.dat"), np.array(y_train))
        np.save(os.path.join(todir, "y_test.dat"), np.array(y_test))
        np.save(os.path.join(todir, "x_train.dat"), np.array(X_train))
        np.save(os.path.join(todir, "x_test.dat"), np.array(X_test))

    @staticmethod
    def load_offline_training_data(fromdir):
        return {
            'train_label': np.load(os.path.join(fromdir, "y_train.dat")),
            'train_data': np.load(os.path.join(fromdir, "x_train.dat")),
            'test_label': np.load(os.path.join(fromdir, "y_test.dat")),
            'test_data': np.load(os.path.join(fromdir, "x_test.dat")),
        }

    def train_model_with_eval(self, train_set, test_set):
        # session_data: {user: [(labels: np.array, features: np.array)]}
        # we use first n - 1 sessions for training and remaining one for testing

        train_label, train_data = train_set
        # generate test set
        test_label, test_data = test_set

        if len(train_label.shape) < 2:
            train_label = sparse_label_converter(train_label)
        if len(test_label.shape) < 2:
            test_label = sparse_label_converter(test_label)

        if self.model is None:
            self.model = DeepGaitModel(
                {'n_classes': train_label.shape[1],
                 'n_hidden': 64}
            )
        self.model.compile(
            optimizer=tf.keras.optimizers.Adam(),  # Optimizer
            # Loss function to minimize
            loss=tf.losses.CategoricalCrossentropy(),
            # List of metrics to monitor
            metrics=[tf.keras.metrics.CategoricalAccuracy()],
        )
        self.model.fit(
            train_data,
            train_label,
            batch_size=self.profile['batch_size'],
            epochs=self.profile['epochs'],
            validation_split=0.1,
            verbose=0
        )

        # model pre-evaluation
        pred = self.model.predict(test_data)

        report = dict()
        # metrics: per-class auc, eer, precision, recall, f1-score, overall acc
        converted_y_pred = np.argmax(pred, axis=1)
        converted_y_true = np.matmul(test_label,
                                     np.array(range(test_label.shape[1])))
        
        
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

        # return pre-evaluation report
        self.trained = True
        return report

    def authenticate(self, features):
        return self.model.predict(features)

    def get_final_layer(self, features):
        return self.model.get_final_layer(features)

    def load_model(self, path):
        self.model = tf.keras.models.load_model(path)
        self.trained = True

    def save_model(self, path):
        assert self.trained == True
        self.model.save(path)

    def __init__(self, **kwargs):
        # TODO: Configure Authenticators
        self.trained = False
        if 'profile' in kwargs:
            self.profile = kwargs['profile']
        else:
            self.profile = {
                'batch_size': 512,
                'epochs': 50
            }

        if 'model' in kwargs:
            self.model = kwargs['model']
        elif 'model_path' in kwargs:
            self.load_model(kwargs['model_path'])
        else:
            self.model = None



"""
Essential functions
"""


def fixed_gait_segmentation_128_old(data):
    # data shape: data length*6
    # step zero: get magnitude of x
    magn = np.sqrt([data[i, 0] ** 2 + data[i, 1] ** 2 + data[i, 2] ** 2
                    for i in range(data.shape[0])])

    # step one: find out all local maxima
    peaks, _ = find_peaks(magn, distance=25, height=10)
    result = []

    # step two: chop by peaks (size: 2 steps, overlap: 1 step)
    for i in range(0, len(peaks) - 3, 1):
        sel_data = data[peaks[i]: peaks[i + 2], :]
        # step three: interpolation
        x = np.arange(0, 128, 128 / sel_data.shape[0])
        if (len(x) == sel_data.shape[0] + 1):
            x = x[:-1]
        x[-1] = 127
        xvals = np.arange(0, 128, 1)
        # print(x[-1], xvals[-1])
        # print(len(x), sel_data.shape[0])
        it = interp1d(x, sel_data.transpose())
        interp_data = it(xvals)

        result.append(interp_data)

    return np.array(result).transpose((0, 2, 1))

def fixed_gait_segmentation_128(data):
    # data shape: data length*6
    # step zero: get magnitude of x
    # TODO: need to update the library
    magn = np.sqrt([data[i, 0] ** 2 + data[i, 1] ** 2 + data[i, 2] ** 2
                    for i in range(data.shape[0])])

    # step one: find out all local maxima
    peaks, _ = find_peaks(magn, distance=25, height=10)
    result = []

    # step two: chop by peaks (size: 2 steps, overlap: 1 step)
    for i in range(0, len(peaks) - 3, 1):
        sel_data = data[peaks[i]: peaks[i + 2], :]
        # step three: interpolation
        x = np.arange(0, 128, 128 / sel_data.shape[0])
        if (len(x) == sel_data.shape[0] + 1):
            x = x[:-1]
        x[-1] = 127
        xvals = np.arange(0, 128, 1)
        # print(x[-1], xvals[-1])
        # print(len(x), sel_data.shape[0])
        it = interp1d(x, sel_data.transpose())
        interp_data = it(xvals)

        result.append(interp_data)
    
    if len(result) == 0:
        return None
    
    return np.array(result).transpose((0, 2, 1))

