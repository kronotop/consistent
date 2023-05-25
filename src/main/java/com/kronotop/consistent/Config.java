// Copyright (c) 2023 Burak Sezer
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.kronotop.consistent;

public class Config {
    public static int DEFAULT_PARTITION_COUNT = 271;
    public static int DEFAULT_REPLICATION_FACTOR = 20;
    public static double DEFAULT_LOAD_FACTOR = 1.25;

    private int partitionCount;
    private int replicationFactor;
    private double loadFactor;

    public Config() {
        partitionCount = DEFAULT_PARTITION_COUNT;
        replicationFactor = DEFAULT_REPLICATION_FACTOR;
        loadFactor = DEFAULT_LOAD_FACTOR;
    }

    public void setLoadFactor(double loadFactor) {
        this.loadFactor = loadFactor;
    }

    public void setPartitionCount(int partitionCount) {
        this.partitionCount = partitionCount;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public double getLoadFactor() {
        return loadFactor;
    }

    public int getPartitionCount() {
        return partitionCount;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }
}
