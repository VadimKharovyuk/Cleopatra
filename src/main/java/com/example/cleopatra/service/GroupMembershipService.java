package com.example.cleopatra.service;

import com.example.cleopatra.dto.GroupDto.*;

import java.util.List;


public interface GroupMembershipService {

    GroupSettingsDto getGroupSettings(Long groupId, Long currentUserId);
    MembershipActionResponse rejectMembership(Long membershipId, Long currentUserId, String reason);
    MembershipActionResponse approveMembership(Long membershipId, Long currentUserId);
    MembershipActionResponse banMember(BanMemberRequest request, Long currentUserId);
    MembershipActionResponse unbanMember(Long membershipId, Long currentUserId);
    MembershipActionResponse changeRole(ChangeRoleRequest request, Long currentUserId);
    MembershipActionResponse removeMember(Long membershipId, Long currentUserId, String reason);

    GroupMembersListDto getAllGroupMembers(Long groupId, Long currentUserId);

    GroupMembersListDto getAllGroupMembers(Long groupId, Long currentUserId,
                                           String roleFilter, String sortBy, String sortOrder,
                                           Integer page, Integer size);

    GroupMembersListDto searchGroupMembers(Long groupId, Long currentUserId, MemberSearchRequest request);

    List<GroupMemberSummaryDto> getGroupMembersSummary(Long groupId, Long currentUserId);







}
