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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Consistent {
    private final Config config;
    private final Hashing hashing;
    private final TreeSet<Integer> sortedSet = new TreeSet<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final HashMap<String, Double> loads = new HashMap<>();
    private final HashMap<String, Member> members = new HashMap<>();
    private final HashMap<Integer, Member> partitions = new HashMap<>();
    private final HashMap<Integer, Member> ring = new HashMap<>();

    public Consistent(Config config, Hashing hashing) {
        this.config = config;
        this.hashing = hashing;
    }

    public Consistent(Config config, Hashing hashing, List<Member> members) {
        this(config, hashing);
        if (members != null && members.size() > 0) {
            for (Member member : members) {
                addMemberInternal(member);
            }
            distributePartitions();
        }
    }

    private int toPositive(int number) {
        return number & 0x7fffffff;
    }

    private int hash32(String key) {
        return toPositive(hashing.hash32(key));
    }

    private double averageLoadInternal() {
        if (members.size() == 0) {
            return 0;
        }
        double avgLoad = ((double) config.getPartitionCount() / members.size()) * config.getLoadFactor();
        return Math.ceil(avgLoad);
    }


    private void distributeWithLoad(int partID, Integer idx, HashMap<Integer, Member> newPartitions, HashMap<String, Double> newLoads) {
        double avgLoad = averageLoadInternal();
        int count = 0;
        while (true) {
            count++;
            if (count >= sortedSet.size()) {
                // User needs to decrease partition count, increase member count or increase load factor.
                throw new RuntimeException("not enough room to distribute partitions");
            }

            Member member = ring.get(idx);
            if (member == null) {
                // TODO: ??
                throw new RuntimeException("member is missing");
            }
            Double load = newLoads.get(member.getId());
            if (load == null) {
                load = 0.0;
            }
            if (load + 1 <= avgLoad) {
                newPartitions.put(partID, member);
                newLoads.put(member.getId(), load + 1);
                return;
            }
            idx = sortedSet.ceiling(idx + 1);
            if (idx == null) {
                idx = sortedSet.first();
            }
        }
    }

    private void distributePartitions() {
        HashMap<String, Double> newLoads = new HashMap<>();
        HashMap<Integer, Member> newPartitions = new HashMap<>();

        if (members.size() > 0) {
            for (int partID = 0; partID < config.getPartitionCount(); partID++) {
                int key = hash32(Integer.toString(partID));
                Integer idx = sortedSet.ceiling(key);
                if (idx == null) {
                    idx = sortedSet.first();
                }
                distributeWithLoad(partID, idx, newPartitions, newLoads);
            }
        }

        partitions.clear();
        partitions.putAll(newPartitions);

        loads.clear();
        loads.putAll(newLoads);
    }

    private void addMemberInternal(Member member) {
        for (int i = 0; i < config.getReplicationFactor(); i++) {
            String key = String.format("%s%d", member.getId(), i);
            int h = hash32(key);
            ring.put(h, member);
            sortedSet.add(h);
        }
        // Storing member at this map is useful to find backup members of a partition.
        members.put(member.getId(), member);
    }

    // Add adds a new member to the consistent hash circle.
    public void addMember(Member member) {
        lock.writeLock().lock();
        try {
            if (members.containsKey(member.getId())) {
                // We already have this member. Quit immediately.
                return;
            }
            addMemberInternal(member);
            distributePartitions();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int findPartition(String key) {
        return hash32(key) % config.getPartitionCount();
    }

    private Member getPartitionOwnerInternal(int partID) {
        Member member = partitions.get(partID);
        if (member == null) {
            throw new NoPartitionOwnerFoundException();
        }
        return member;
    }

    public Member getPartitionOwner(int partID) {
        lock.readLock().lock();
        try {
            return getPartitionOwnerInternal(partID);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Member locate(String key) {
        int partID = findPartition(key);
        return getPartitionOwner(partID);
    }

    public double averageLoad() {
        lock.readLock().lock();
        try {
            return averageLoadInternal();
        } finally {
            lock.readLock().unlock();
        }
    }

    public HashMap<Member, Double> loadDistribution() {
        lock.readLock().lock();
        try {
            HashMap<Member, Double> result = new HashMap<>();
            for (String id : members.keySet()) {
                Member member = members.get(id);
                Double load = loads.get(id);
                if (load == null) {
                    load = 0.0;
                }
                result.put(member, load);
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Member> getMembers() {
        lock.readLock().lock();
        try {
            List<Member> result = new ArrayList<>();
            for (String id : members.keySet()) {
                result.add(members.get(id));
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void removeMember(Member member) {
        lock.writeLock().lock();
        try {
            if (!members.containsKey(member.getId())) {
                return;
            }
            for (int i = 0; i < config.getReplicationFactor(); i++) {
                String key = String.format("%s%d", member.getId(), i);
                int h = hash32(key);
                ring.remove(h);
                sortedSet.remove(h);
            }
            members.remove(member.getId());
            distributePartitions();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
