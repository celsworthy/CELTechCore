/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.gcodetranslator;

/**
 *
 * @author Ian
 */
class PostProcessingError extends Exception
{
    public PostProcessingError(String exceptionInformation)
    {
        super(exceptionInformation);
    }
}
