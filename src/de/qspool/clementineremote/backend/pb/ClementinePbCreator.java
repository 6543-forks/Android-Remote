/* This file is part of the Android Clementine Remote.
 * Copyright (C) 2013, Andreas Muttscheller <asfa194@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.qspool.clementineremote.backend.pb;

import de.qspool.clementineremote.App;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.DownloadItem;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.Message;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.MsgType;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.Repeat;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.RequestChangeSong;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.RequestConnect;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.RequestDownloadSongs;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.RequestPlaylistSongs;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.RequestSetTrackPosition;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.RequestSetVolume;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.ResponseSongOffer;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.Shuffle;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.ShuffleMode;

/**
 * Creates the protocol buffer messages
 */
public class ClementinePbCreator {
	/**
	 * Create a song offer response
	 * @param msg The message itself
	 * @param r A SongOfferResponse
	 * @return ResponseSongOffer Builder for protocol buffer message
	 */
	public static ClementineMessage buildSongOfferResponse(boolean accepted) {
		Message.Builder msg = ClementineMessage.getMessageBuilder(MsgType.SONG_OFFER_RESPONSE);
		ResponseSongOffer.Builder offer = msg.getResponseSongOfferBuilder();
		offer.setAccepted(accepted);
		return new ClementineMessage(msg);
	}

	/**
	 * Create a download song message
	 * @param msg The message itself
	 * @param r The download request
	 * @return The built request
	 */
	public static ClementineMessage buildDownloadSongsMessage(int playlistId, DownloadItem downloadItem) {
		Message.Builder msg = ClementineMessage.getMessageBuilder(MsgType.DOWNLOAD_SONGS);
		RequestDownloadSongs.Builder request = msg.getRequestDownloadSongsBuilder();
		
		request.setPlaylistId(playlistId);
		request.setDownloadItem(downloadItem);
		
		return new ClementineMessage(msg);
	}

	/**
	 * Create the volume specific message
	 * @param msg The Message itself
	 * @param r The Request
	 * @return the Volume message part
	 */
	public static ClementineMessage buildVolumeMessage(int volume) {
		Message.Builder msg = ClementineMessage.getMessageBuilder(MsgType.SET_VOLUME);
		
		RequestSetVolume.Builder requestSetVolume = msg.getRequestSetVolumeBuilder();
		requestSetVolume.setVolume(volume);
		
		return new ClementineMessage(msg);
	}
	
	/**
	 * Create the connect specific message
	 * @param msg The Message itself
	 * @param r The Request
	 * @return the connect message part
	 */
	public static ClementineMessage buildConnectMessage(String ip, int port, int authCode, boolean getPlaylistSongs, boolean isDownloader) {
		Message.Builder msg = ClementineMessage.getMessageBuilder(MsgType.CONNECT);
		
		RequestConnect.Builder requestConnect = msg.getRequestConnectBuilder();
		
		requestConnect.setAuthCode(authCode);
		requestConnect.setSendPlaylistSongs(getPlaylistSongs);
		requestConnect.setDownloader(isDownloader);
		
		ClementineMessage clementineMessage = new ClementineMessage(msg);
		clementineMessage.setIp(ip);
		clementineMessage.setPort(port);
		
		return clementineMessage;
	}
	
	/**
	 * Build shuffle Message
	 * @param msg The root message
	 * @return The created element
	 */
	public static ClementineMessage buildShuffle() {
		Message.Builder msg = ClementineMessage.getMessageBuilder(MsgType.SHUFFLE);
		
		Shuffle.Builder shuffle = msg.getShuffleBuilder();
		
		switch (App.mClementine.getShuffleMode()) {
		case OFF: 		shuffle.setShuffleMode(ShuffleMode.Shuffle_Off);
						break;
		case ALL:		shuffle.setShuffleMode(ShuffleMode.Shuffle_All);
						break;
		case INSIDE_ALBUM:	shuffle.setShuffleMode(ShuffleMode.Shuffle_InsideAlbum);
							break;
		case ALBUMS:	shuffle.setShuffleMode(ShuffleMode.Shuffle_Albums);
						break;
		}
		return new ClementineMessage(msg);
	}

	/**
	 * Build Repeat Message
	 * @param msg The root message
	 * @return The created element
	 */
	public static ClementineMessage buildRepeat() {
		Message.Builder msg = ClementineMessage.getMessageBuilder(MsgType.REPEAT);
		
		Repeat.Builder repeat = msg.getRepeatBuilder();
		
		switch (App.mClementine.getRepeatMode()) {
		case OFF: 		repeat.setRepeatMode(ClementineRemoteProtocolBuffer.RepeatMode.Repeat_Off);
						break;
		case TRACK:		repeat.setRepeatMode(ClementineRemoteProtocolBuffer.RepeatMode.Repeat_Track);
						break;
		case ALBUM:		repeat.setRepeatMode(ClementineRemoteProtocolBuffer.RepeatMode.Repeat_Album);
						break;
		case PLAYLIST:	repeat.setRepeatMode(ClementineRemoteProtocolBuffer.RepeatMode.Repeat_Playlist);
						break;
		}
		return new ClementineMessage(msg);
	}
	
	/**
	 * Request all Songs in current playlist
	 * @param msg The root message
	 * @param r The Request Object
	 * @return The Builder for the Message
	 */
	public static ClementineMessage buildRequestPlaylistSongs(int playlistId) {
		Message.Builder msg = ClementineMessage.getMessageBuilder(MsgType.REQUEST_PLAYLIST_SONGS);
		
		RequestPlaylistSongs.Builder requestPlaylistSongs = msg.getRequestPlaylistSongsBuilder();
		
		requestPlaylistSongs.setId(playlistId);
		
		return new ClementineMessage(msg);
	}
	
	/**
	 * Request all Songs in current playlist
	 * @param msg The root message
	 * @param r The Request Object
	 * @return The Builder for the Message
	 */
	public static ClementineMessage buildRequestChangeSong(int songIndex, int playlistId) {
		Message.Builder msg = ClementineMessage.getMessageBuilder(MsgType.CHANGE_SONG);
		
		RequestChangeSong.Builder request = msg.getRequestChangeSongBuilder();
		
		request.setSongIndex(songIndex);
		request.setPlaylistId(playlistId);
		
		return new ClementineMessage(msg);
	}
	
	/**
	 * Request to set the track position
	 * @param msg The root message
	 * @return 
	 */
	public static ClementineMessage buildTrackPosition(int position) {
		Message.Builder msg = ClementineMessage.getMessageBuilder(MsgType.SET_TRACK_POSITION);
		
		RequestSetTrackPosition.Builder request = msg.getRequestSetTrackPositionBuilder();
		request.setPosition(position);
		
		return new ClementineMessage(msg);
	}
}
