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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.google.common.hash.Hashing.murmur3_32_fixed;
import static org.junit.jupiter.api.Assertions.*;

public class ConsistentTest {
    protected final Config config = new Config();
    protected final Hashing hashing = key -> murmur3_32_fixed().hashString(key, StandardCharsets.US_ASCII).asInt();

    @Test
    public void testAdd() {
        Consistent consistent = new Consistent(config, hashing);

        Member member = new MockMemberImpl("member-1");
        consistent.addMember(member);

        Member targetMember = consistent.locate("foobar");
        assertEquals(member.getId(), targetMember.getId());
    }

    @Test
    public void testGetMembers() {
        Consistent consistent = new Consistent(config, hashing);

        Member memberOne = new MockMemberImpl("member-1");
        consistent.addMember(memberOne);

        Member memberTwo = new MockMemberImpl("member-2");
        consistent.addMember(memberTwo);

        List<Member> expectedMembers = new ArrayList<>();
        expectedMembers.add(memberOne);
        expectedMembers.add(memberTwo);

        List<Member> members = consistent.getMembers();
        assertEquals(2, members.size());

        assertTrue(expectedMembers.containsAll(members));
    }

    @Test
    public void testAverageLoad() {
        Consistent consistent = new Consistent(config, hashing);

        Member member = new MockMemberImpl("member-1");
        consistent.addMember(member);

        assertTrue(consistent.averageLoad() > 0);
    }


    @Test
    public void testGetPartitionOwner() {
        Consistent consistent = new Consistent(config, hashing);

        Member memberOne = new MockMemberImpl("member-1");
        consistent.addMember(memberOne);

        Member memberTwo = new MockMemberImpl("member-2");
        consistent.addMember(memberTwo);

        List<Member> expectedMembers = new ArrayList<>();
        expectedMembers.add(memberOne);
        expectedMembers.add(memberTwo);

        List<Member> result = new ArrayList<>();
        for (int partID = 0; partID < config.getPartitionCount(); partID++) {
            Member owner = consistent.getPartitionOwner(partID);
            result.add(owner);
        }
        assertTrue(expectedMembers.containsAll(result));
    }

    @Test
    public void testLocate_EmptyHashRing() {
        Consistent consistent = new Consistent(config, hashing);
        NoPartitionOwnerFoundException exception = assertThrows(
                NoPartitionOwnerFoundException.class,
                () -> consistent.locate("foobar")
        );
        assertNotNull(exception);
    }

    @Test
    public void testLocate() {
        Consistent consistent = new Consistent(config, hashing);

        Member memberOne = new MockMemberImpl("member-1");
        consistent.addMember(memberOne);

        Member memberTwo = new MockMemberImpl("member-2");
        consistent.addMember(memberTwo);

        Set<String> members = new HashSet<>();
        members.add(memberOne.getId());
        members.add(memberTwo.getId());

        Member owner = consistent.locate("foobar");
        assertTrue(members.contains(owner.getId()));
    }

    @Test
    public void testLoadDistribution() {
        Consistent consistent = new Consistent(config, hashing);

        for (int i = 1; i <= 10; i++) {
            Member member = new MockMemberImpl(String.format("member-%d", i));
            consistent.addMember(member);
        }

        double averageLoad = consistent.averageLoad();
        HashMap<Member, Double> loads = consistent.loadDistribution();
        for (Double load : loads.values()) {
            assertTrue(load <= averageLoad);
        }
    }

    @Test
    public void testRemoveMember_EmptyHashRing() {
        Consistent consistent = new Consistent(config, hashing);

        Member member = new MockMemberImpl("member-1");
        consistent.addMember(member);

        assertDoesNotThrow(() -> consistent.removeMember(member));
    }

    @Test
    public void testRemove() {
        Consistent consistent = new Consistent(config, hashing);

        Member memberOne = new MockMemberImpl("member-1");
        consistent.addMember(memberOne);

        Member memberTwo = new MockMemberImpl("member-2");
        consistent.addMember(memberTwo);

        consistent.removeMember(memberTwo);

        Member owner = consistent.locate("foobar");
        assertEquals(memberOne, owner);
    }

    @Test
    public void testConsistentWithInitialMembers() {
        List<Member> members = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Member member = new MockMemberImpl(String.format("member-%d", i));
            members.add(member);
        }

        Consistent consistent = new Consistent(config, hashing, members);
        List<Member> currentMembers = consistent.getMembers();
        assertEquals(10, members.size());
        assertTrue(currentMembers.containsAll(members));
    }
}
